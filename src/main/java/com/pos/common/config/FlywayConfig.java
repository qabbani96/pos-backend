package com.pos.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Automatically repairs Flyway's migration history before running migrations.
 *
 * Why this exists:
 *   If a migration fails mid-execution (e.g. SQL syntax error, column already exists),
 *   Flyway records a "success=false" row in flyway_schema_history.  On the next startup
 *   Flyway refuses to proceed until the failed entry is manually resolved.
 *
 *   repair() removes those failed entries so the corrected migration can run cleanly
 *   on the next startup — no manual DB intervention required.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();   // clears success=false entries from flyway_schema_history
            flyway.migrate();
        };
    }
}
