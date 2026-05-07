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
        Integer sampleBookCount = jdbcTemplate.queryForObject(
                "select count(*) from book where note like 'Sample record seeded for demonstration%'",
                Map.of(),
                Integer.class);
        Integer sampleBooksWithoutCollections = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from book b
                        where b.note like 'Sample record seeded for demonstration%'
                          and not exists (
                              select 1
                              from book_collection bc
                              where bc.book_id = b.id
                          )
                        """,
                Map.of(),
                Integer.class);
        Integer sampleBooksWithoutBestEstimate = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from book b
                        where b.note like 'Sample record seeded for demonstration%'
                          and coalesce(nullif(b.price_to_replace, 0), nullif(b.price_paid, 0), nullif(b.price_on_item, 0), 0) = 0
                        """,
                Map.of(),
                Integer.class);
        Integer sampleUpdatedTimestampCount = jdbcTemplate.queryForObject(
                "select count(distinct updated_at) from book where note like 'Sample record seeded for demonstration%'",
                Map.of(),
                Integer.class);

        assertThat(flywayHistoryCount).isGreaterThanOrEqualTo(11);
        assertThat(collectionCount).isGreaterThanOrEqualTo(5);
        assertThat(shelfCount).isGreaterThanOrEqualTo(5);
        assertThat(sampleBookCount).isGreaterThanOrEqualTo(8);
        assertThat(sampleBooksWithoutCollections).isGreaterThanOrEqualTo(1);
        assertThat(sampleBooksWithoutBestEstimate).isGreaterThanOrEqualTo(1);
        assertThat(sampleUpdatedTimestampCount).isGreaterThanOrEqualTo(3);
    }
}
