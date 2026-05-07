package com.baroquepotion.bookshelves;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void valueSummaryReturnsTotalsCoverageAndReplacementDataQuality() {
        insertBook("Alpha", "Author One", "shelf-a", "general", false, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(15), LocalDate.of(2026, 3, 21));
        insertBook("Beta", "Author Two", "shelf-a", "general", true, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(25), null, null);
        insertBook("Gamma", "Author Three", null, "bestsellers", true, true,
                BigDecimal.valueOf(30), BigDecimal.valueOf(35), BigDecimal.valueOf(40), null);
        insertBook("Delta", "Author Four", "shelf-c", "bestsellers", false, true,
                BigDecimal.valueOf(50), null, BigDecimal.valueOf(60), LocalDate.of(2026, 3, 18));

        ResponseEntity<Map> paidSummary = restTemplate.getForEntity("/api/analytics/value-summary?basis=paid", Map.class);

        assertThat(paidSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paidSummary.getBody())
                .containsEntry("basis", "paid")
                .containsEntry("includedCount", 4)
                .containsEntry("missingValueCount", 0)
                .containsEntry("totalScopedCount", 4)
                .containsEntry("coveragePercent", 100.00);
        assertThat(new BigDecimal(String.valueOf(paidSummary.getBody().get("totalValue")))).isEqualByComparingTo("110");

        ResponseEntity<Map> replacementSummary = restTemplate.getForEntity("/api/analytics/value-summary?basis=replacement", Map.class);
        assertThat(replacementSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replacementSummary.getBody())
                .containsEntry("includedCount", 3)
                .containsEntry("missingValueCount", 1)
                .containsEntry("totalScopedCount", 4)
                .containsEntry("coveragePercent", 75.00)
                .containsEntry("missingReplacementCheckedCount", 1)
                .containsEntry("oldestReplacementChecked", "2026-03-18");
        assertThat(new BigDecimal(String.valueOf(replacementSummary.getBody().get("totalValue")))).isEqualByComparingTo("115");

        ResponseEntity<Map> specialHardbackPaid = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=paid&specialOnly=true&hardbackOnly=true", Map.class);
        assertThat(specialHardbackPaid.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(specialHardbackPaid.getBody())
                .containsEntry("includedCount", 3)
                .containsEntry("missingValueCount", 0)
                .containsEntry("totalScopedCount", 3)
                .containsEntry("coveragePercent", 100.00);
        assertThat(new BigDecimal(String.valueOf(specialHardbackPaid.getBody().get("totalValue")))).isEqualByComparingTo("100");
    }

    @Test
    void valueDetailsReturnsScopedBooksWithSelectedValueBasis() {
        insertBook("Paper Survey", "Anne Author", "shelf-a", "general", false, false,
                BigDecimal.valueOf(11), BigDecimal.valueOf(14), BigDecimal.valueOf(16), LocalDate.of(2026, 3, 12));
        insertBook("Ink Survey", "Brian Bibliophile", "shelf-a", "general", true, false,
                BigDecimal.valueOf(22), BigDecimal.valueOf(27), null, null);
        insertBook("Shelf Outlier", "Cara Collector", "shelf-c", "bestsellers", false, true,
                BigDecimal.valueOf(44), BigDecimal.valueOf(48), BigDecimal.valueOf(52), LocalDate.of(2026, 3, 10));

        ResponseEntity<Map> details = restTemplate.getForEntity(
                "/api/analytics/value-details?basis=printed&collectionId=general", Map.class);

        assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(details.getBody()).containsEntry("basis", "printed")
                .containsEntry("totalItems", 2);
        List<Map<String, Object>> items = (List<Map<String, Object>>) details.getBody().get("items");
        assertThat(items).extracting(item -> item.get("title"))
                .containsExactly("Paper Survey", "Ink Survey");
        assertThat(items).extracting(item -> item.get("valueAmount"))
                .containsExactly(14.0, 27.0);

        ResponseEntity<Map> hardbackDetails = restTemplate.getForEntity(
                "/api/analytics/value-details?basis=paid&hardbackOnly=true&shelfId=shelf-c", Map.class);
        assertThat(hardbackDetails.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> hardbackItems = (List<Map<String, Object>>) hardbackDetails.getBody().get("items");
        assertThat(hardbackItems).hasSize(1);
        assertThat(hardbackItems.getFirst()).containsEntry("title", "Shelf Outlier")
                .containsEntry("special", false)
                .containsEntry("hardback", true);
    }

    @Test
    void valueAnalyticsSupportsDateAddedBeforeAndAfterFilters() {
        insertBook("Older Added", "Author One", "shelf-a", "general", false, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(15), LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 10));
        insertBook("Newer Added", "Author Two", "shelf-a", "general", false, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(22), BigDecimal.valueOf(25), LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 20));
        insertBook("No Added Date", "Author Three", "shelf-a", "general", false, false,
                BigDecimal.valueOf(30), BigDecimal.valueOf(32), BigDecimal.valueOf(35), LocalDate.of(2026, 3, 23), null);

        ResponseEntity<Map> beforeSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=paid&dateAddedMode=before&dateAddedOn=2026-03-15", Map.class);
        assertThat(beforeSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(beforeSummary.getBody())
                .containsEntry("includedCount", 1)
                .containsEntry("totalScopedCount", 1);
        assertThat(new BigDecimal(String.valueOf(beforeSummary.getBody().get("totalValue")))).isEqualByComparingTo("10");

        ResponseEntity<Map> afterDetails = restTemplate.getForEntity(
                "/api/analytics/value-details?basis=paid&dateAddedMode=after&dateAddedOn=2026-03-15", Map.class);
        assertThat(afterDetails.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterDetails.getBody()).containsEntry("totalItems", 1);
        List<Map<String, Object>> items = (List<Map<String, Object>>) afterDetails.getBody().get("items");
        assertThat(items).extracting(item -> item.get("title")).containsExactly("Newer Added");

        ResponseEntity<Map> yearSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=paid&dateAddedMode=year&dateAddedYear=2026", Map.class);
        assertThat(yearSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(yearSummary.getBody())
                .containsEntry("includedCount", 2)
                .containsEntry("totalScopedCount", 2);
        assertThat(new BigDecimal(String.valueOf(yearSummary.getBody().get("totalValue")))).isEqualByComparingTo("30");
    }

    @Test
    void valueAnalyticsSupportsDigitalScopeFilter() {
        UUID physicalBookId = insertBook("Physical Only", "Author One", "shelf-a", "general", false, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(15), LocalDate.of(2026, 3, 21));
        UUID digitalBookId = insertBook("Digital Only", "Author Two", "shelf-a", "general", false, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(22), BigDecimal.valueOf(25), LocalDate.of(2026, 3, 22));

        jdbcTemplate.update("update book set digital = true where id = :id", Map.of("id", digitalBookId));
        jdbcTemplate.update("update book set digital = false where id = :id", Map.of("id", physicalBookId));

        ResponseEntity<Map> digitalSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=paid&digitalOnly=true", Map.class);
        assertThat(digitalSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(digitalSummary.getBody())
                .containsEntry("includedCount", 1)
                .containsEntry("totalScopedCount", 1);
        assertThat(new BigDecimal(String.valueOf(digitalSummary.getBody().get("totalValue")))).isEqualByComparingTo("20");
    }

    @Test
    void valueAnalyticsSupportsDustjacketSlipcaseAndVideoScopeFilters() {
        UUID plainBookId = insertBook("Plain Only", "Author One", "shelf-a", "general", false, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(15), LocalDate.of(2026, 3, 21));
        UUID boxedBookId = insertBook("Boxed Video", "Author Two", "shelf-a", "general", false, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(22), BigDecimal.valueOf(25), LocalDate.of(2026, 3, 22));

        jdbcTemplate.update("update book set dustjacket = false, slipcase = false, video = false where id = :id", Map.of("id", plainBookId));
        jdbcTemplate.update("update book set dustjacket = true, slipcase = true, video = true where id = :id", Map.of("id", boxedBookId));

        ResponseEntity<Map> dustjacketSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=paid&dustjacketOnly=true", Map.class);
        assertThat(dustjacketSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dustjacketSummary.getBody())
                .containsEntry("includedCount", 1)
                .containsEntry("totalScopedCount", 1);
        assertThat(new BigDecimal(String.valueOf(dustjacketSummary.getBody().get("totalValue")))).isEqualByComparingTo("20");

        ResponseEntity<Map> slipcaseVideoDetails = restTemplate.getForEntity(
                "/api/analytics/value-details?basis=paid&slipcaseOnly=true&videoOnly=true", Map.class);
        assertThat(slipcaseVideoDetails.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> items = (List<Map<String, Object>>) slipcaseVideoDetails.getBody().get("items");
        assertThat(items).hasSize(1);
        assertThat(items.getFirst())
                .containsEntry("title", "Boxed Video")
                .containsEntry("dustjacket", true)
                .containsEntry("slipcase", true)
                .containsEntry("video", true);
    }

    @Test
    void bestEstimateBasisFallsBackFromReplacementToPaidToPrintedThenZero() {
        insertBook("Replacement First", "Author One", "shelf-a", "general", false, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(15), LocalDate.of(2026, 3, 21));
        insertBook("Paid Fallback", "Author Two", "shelf-a", "general", false, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(25), null, null);
        insertBook("Printed Fallback", "Author Three", "shelf-a", "general", false, false,
                BigDecimal.ZERO, BigDecimal.valueOf(35), BigDecimal.ZERO, null);
        insertBook("Zero Fallback", "Author Four", "shelf-a", "general", false, false,
                BigDecimal.ZERO, null, null, null);

        ResponseEntity<Map> summary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=bestEstimate", Map.class);

        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summary.getBody())
                .containsEntry("basis", "bestEstimate")
                .containsEntry("includedCount", 4)
                .containsEntry("missingValueCount", 0)
                .containsEntry("totalScopedCount", 4)
                .containsEntry("coveragePercent", 100.00);
        assertThat(new BigDecimal(String.valueOf(summary.getBody().get("totalValue")))).isEqualByComparingTo("70");

        ResponseEntity<Map> details = restTemplate.getForEntity(
                "/api/analytics/value-details?basis=bestEstimate", Map.class);
        assertThat(details.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(details.getBody()).containsEntry("basis", "bestEstimate")
                .containsEntry("totalItems", 4);
        List<Map<String, Object>> items = (List<Map<String, Object>>) details.getBody().get("items");
        assertThat(items).extracting(item -> item.get("title"))
                .containsExactly("Zero Fallback", "Replacement First", "Printed Fallback", "Paid Fallback");
        assertThat(items).extracting(item -> item.get("valueAmount"))
                .containsExactly(0, 15.0, 35.0, 20.0);
    }

    @Test
    void excludedBooksCountAsZeroExceptWhenExcludedCollectionIsSelected() {
        ensureExcludedCollection();
        insertBook("Ordinary General", "Author One", "shelf-a", "general", false, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(15), LocalDate.of(2026, 3, 21));
        UUID excludedBookId = insertBook("Excluded General", "Author Two", "shelf-a", "general", false, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(22), BigDecimal.valueOf(25), LocalDate.of(2026, 3, 22));
        jdbcTemplate.update(
                "insert into book_collection (book_id, collection_id) values (:bookId, 'excluded')",
                Map.of("bookId", excludedBookId));

        ResponseEntity<Map> defaultSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=bestEstimate", Map.class);
        assertThat(defaultSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(defaultSummary.getBody())
                .containsEntry("includedCount", 2)
                .containsEntry("missingValueCount", 0)
                .containsEntry("totalScopedCount", 2);
        assertThat(new BigDecimal(String.valueOf(defaultSummary.getBody().get("totalValue")))).isEqualByComparingTo("15");

        ResponseEntity<Map> generalSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=bestEstimate&collectionId=general", Map.class);
        assertThat(generalSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new BigDecimal(String.valueOf(generalSummary.getBody().get("totalValue")))).isEqualByComparingTo("15");

        ResponseEntity<Map> generalDetails = restTemplate.getForEntity(
                "/api/analytics/value-details?basis=bestEstimate&collectionId=general", Map.class);
        List<Map<String, Object>> generalItems = (List<Map<String, Object>>) generalDetails.getBody().get("items");
        assertThat(generalItems).extracting(item -> item.get("title"))
                .containsExactly("Ordinary General", "Excluded General");
        assertThat(generalItems).extracting(item -> item.get("valueAmount"))
                .containsExactly(15.0, 0);

        ResponseEntity<Map> excludedSummary = restTemplate.getForEntity(
                "/api/analytics/value-summary?basis=bestEstimate&collectionId=excluded", Map.class);
        assertThat(excludedSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(excludedSummary.getBody()).containsEntry("totalScopedCount", 1);
        assertThat(new BigDecimal(String.valueOf(excludedSummary.getBody().get("totalValue")))).isEqualByComparingTo("25");
    }

    private void ensureExcludedCollection() {
        jdbcTemplate.update("""
                insert into collection (id, name)
                values ('excluded', 'Excluded')
                on conflict (id) do nothing
                """, Map.of());
    }

    private UUID insertBook(String title,
                            String author,
                            String shelfId,
                            String collectionId,
                            boolean special,
                            boolean hardback,
                            BigDecimal pricePaid,
                            BigDecimal priceOnItem,
                            BigDecimal priceToReplace,
                            LocalDate priceToReplaceChecked) {
        return insertBook(title, author, shelfId, collectionId, special, hardback, pricePaid, priceOnItem, priceToReplace, priceToReplaceChecked, LocalDate.of(2026, 3, 20));
    }

    private UUID insertBook(String title,
                            String author,
                            String shelfId,
                            String collectionId,
                            boolean special,
                            boolean hardback,
                            BigDecimal pricePaid,
                            BigDecimal priceOnItem,
                            BigDecimal priceToReplace,
                            LocalDate priceToReplaceChecked,
                            LocalDate dateAdded) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("author", author);
        params.put("publicationCity", null);
        params.put("publisher", "Test Publisher");
        params.put("year", 2020);
        params.put("edition", "1");
        params.put("pages", 250);
        params.put("frontpages", "xii");
        params.put("dateAdded", dateAdded);
        params.put("shelfId", shelfId);
        params.put("authorSecondary", "Second Author");
        params.put("isbn", "1234567890");
        params.put("special", special);
        params.put("hardback", hardback);
        params.put("vols", 1);
        params.put("series", "Test Series");
        params.put("shelfCoords", "A-1");
        params.put("note", "test note");
        params.put("pricePaid", pricePaid);
        params.put("priceOnItem", priceOnItem);
        params.put("priceToReplace", priceToReplace);
        params.put("priceToReplaceChecked", priceToReplaceChecked);
        UUID bookId = jdbcTemplate.queryForObject("""
                        insert into book (
                            title, author, publication_city, publisher, year, edition, pages, frontpages, date_added, shelf_id,
                            author_secondary, isbn, special, hardback, vols, series, shelf_coords, note,
                            price_paid, price_on_item, price_to_replace, price_to_replace_checked
                        ) values (
                            :title, :author, :publicationCity, :publisher, :year, :edition, :pages, :frontpages, :dateAdded, :shelfId,
                            :authorSecondary, :isbn, :special, :hardback, :vols, :series, :shelfCoords, :note,
                            :pricePaid, :priceOnItem, :priceToReplace, :priceToReplaceChecked
                        )
                        returning id
                        """,
                params,
                (rs, rowNum) -> rs.getObject("id", UUID.class));

        jdbcTemplate.update(
                "insert into book_collection (book_id, collection_id) values (:bookId, :collectionId)",
                Map.of("bookId", bookId, "collectionId", collectionId));
        return bookId;
    }
}
