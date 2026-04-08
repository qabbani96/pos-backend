package com.pos.barcode.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.item.entity.Item;
import com.pos.item.entity.Item.BarcodeType;
import com.pos.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeService {

    private final ItemRepository itemRepository;

    // ── Label dimensions (57mm × 32mm — common thermal label roll) ───────────
    private static final float LABEL_WIDTH_PT  = 161.57f;   // 57mm in points
    private static final float LABEL_HEIGHT_PT = 90.71f;    // 32mm in points

    // ── PNG barcode defaults ─────────────────────────────────────────────────
    private static final int DEFAULT_WIDTH  = 300;
    private static final int DEFAULT_HEIGHT = 100;

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Auto-generates a unique barcode value, saves it to the item, returns the item.
     * Idempotent: if the item already has a barcode, just returns it.
     */
    @Transactional
    public Item assignBarcode(Long itemId) {
        Item item = getItemOrThrow(itemId);

        if (item.getBarcode() != null && !item.getBarcode().isBlank()) {
            log.info("action=barcode_already_assigned, itemId={}, barcode={}", itemId, item.getBarcode());
            return item;
        }

        String barcodeValue = generateBarcodeValue(item);
        item.setBarcode(barcodeValue);
        log.info("action=barcode_assigned, itemId={}, barcode={}", itemId, barcodeValue);
        return item;
    }

    /**
     * Returns a PNG byte array for the item's barcode.
     * Requires the item to already have a barcode assigned.
     */
    public byte[] generatePng(Long itemId) {
        Item item = getItemOrThrow(itemId);
        requireBarcode(item);
        return encodeToPng(item.getBarcode(), item.getBarcodeType(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Returns a PDF label byte array — item name + barcode image + price.
     * Suitable for sending to a thermal/ESC-POS label printer.
     */
    public byte[] generatePdfLabel(Long itemId) {
        Item item = getItemOrThrow(itemId);
        requireBarcode(item);

        byte[] barcodePng = encodeToPng(item.getBarcode(), item.getBarcodeType(), 280, 80);

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = new PDRectangle(LABEL_WIDTH_PT, LABEL_HEIGHT_PT);
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            PDImageXObject barcodeImage = PDImageXObject.createFromByteArray(doc, barcodePng, "barcode");

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                drawLabel(cs, item, barcodeImage);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("action=pdf_label_failed, itemId={}, error={}", itemId, e.getMessage());
            throw new BusinessException("PDF_GENERATION_FAILED", "Failed to generate PDF label: " + e.getMessage());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void drawLabel(PDPageContentStream cs, Item item, PDImageXObject barcodeImage)
            throws IOException {

        PDType1Font boldFont    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // Item name — top of label
        String name = truncate(item.getName(), 22);
        cs.beginText();
        cs.setFont(boldFont, 7f);
        cs.newLineAtOffset(4, LABEL_HEIGHT_PT - 10);
        cs.showText(name);
        cs.endText();

        // Barcode image — centre of label
        float imgWidth  = LABEL_WIDTH_PT - 8;
        float imgHeight = 45f;
        float imgY      = LABEL_HEIGHT_PT - 12 - imgHeight;
        cs.drawImage(barcodeImage, 4, imgY, imgWidth, imgHeight);

        // Barcode value text — below image
        cs.beginText();
        cs.setFont(regularFont, 6f);
        float textX = centerTextX(item.getBarcode(), regularFont, 6f);
        cs.newLineAtOffset(textX, imgY - 8);
        cs.showText(item.getBarcode());
        cs.endText();

        // Price — bottom right
        String price = "SAR " + item.getPrice().toPlainString();
        cs.beginText();
        cs.setFont(boldFont, 7f);
        float priceX = LABEL_WIDTH_PT - getTextWidth(price, boldFont, 7f) - 4;
        cs.newLineAtOffset(priceX, 4);
        cs.showText(price);
        cs.endText();
    }

    private byte[] encodeToPng(String value, BarcodeType type, int width, int height) {
        try {
            BarcodeFormat format = toZxingFormat(type);

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(value, format, width, height, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();

        } catch (WriterException | IOException e) {
            log.error("action=barcode_encode_failed, value={}, error={}", value, e.getMessage());
            throw new BusinessException("BARCODE_ENCODE_FAILED", "Failed to encode barcode: " + e.getMessage());
        }
    }

    /**
     * Generates a unique barcode value based on type:
     * - CODE128: "POS" prefix + zero-padded item ID
     * - EAN13:   12-digit numeric string (padded item ID) + computed check digit
     * - QR:      full UUID
     */
    private String generateBarcodeValue(Item item) {
        return switch (item.getBarcodeType()) {
            case CODE128 -> String.format("POS%010d", item.getId());
            case EAN13   -> buildEan13(item.getId());
            case QR      -> UUID.randomUUID().toString();
        };
    }

    /**
     * Builds a valid 13-digit EAN-13 from the item ID (12 digits + check digit).
     */
    private String buildEan13(Long itemId) {
        String base = String.format("%012d", itemId % 1_000_000_000_000L);
        int checkDigit = computeEan13CheckDigit(base);
        return base + checkDigit;
    }

    private int computeEan13CheckDigit(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(twelveDigits.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }

    private BarcodeFormat toZxingFormat(BarcodeType type) {
        return switch (type) {
            case CODE128 -> BarcodeFormat.CODE_128;
            case EAN13   -> BarcodeFormat.EAN_13;
            case QR      -> BarcodeFormat.QR_CODE;
        };
    }

    private void requireBarcode(Item item) {
        if (item.getBarcode() == null || item.getBarcode().isBlank()) {
            throw new BusinessException(
                    "BARCODE_NOT_ASSIGNED",
                    "Item id=" + item.getId() + " has no barcode. Call /generate first.");
        }
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private float getTextWidth(String text, PDType1Font font, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private float centerTextX(String text, PDType1Font font, float fontSize) throws IOException {
        float textWidth = getTextWidth(text, font, fontSize);
        return Math.max(4, (LABEL_WIDTH_PT - textWidth) / 2f);
    }
}
