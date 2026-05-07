package com.baroquepotion.bookshelves;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void migrationsCreateSchemaHistoryAndSeedReferenceData() {
        Integer flywayHistoryCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history",
                Map.of(),
                Integer.class);
        Integer collectionCount = jdbcTemplate.queryForObject(
                "select count(*) from collection",
                Map.of(),
                Integer.class);
        Integer shelfCount = jdbcTemplate.queryForObject(
                "select count(*) from shelf",
                Map.of(),
                Integer.class);

        assertThat(flywayHistoryCount).isGreaterThanOrEqualTo(11);
        assertThat(collectionCount).isGreaterThanOrEqualTo(5);
        assertThat(shelfCount).isGreaterThanOrEqualTo(5);
    }
}
