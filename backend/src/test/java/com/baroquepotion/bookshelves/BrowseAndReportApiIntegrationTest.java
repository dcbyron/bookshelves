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

class BrowseAndReportApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void authorsEndpointAndAuthorBooksEndpointReturnExpectedBuckets() {
        insertBook("Bestsellers for Beginners", "Jane Doe", LocalDate.of(2026, 3, 20), "shelf-a", "bestsellers");
        insertBook("More Bestsellers", "Jane Doe", LocalDate.of(2026, 3, 21), "shelf-a", "bestsellers");
        insertBook("Nameless Volume", null, LocalDate.of(2026, 3, 19), null, "general");

        ResponseEntity<List> authors = restTemplate.getForEntity("/api/browse/authors", List.class);

        assertThat(authors.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authors.getBody()).isNotNull();
        assertThat(authors.getBody()).extracting(item -> ((Map<?, ?>) item).get("name"))
                .contains("Jane Doe", "(Unspecified)");
        String janeKey = null;
        for (Object item : authors.getBody()) {
            Map<?, ?> row = (Map<?, ?>) item;
            if ("Jane Doe".equals(row.get("name"))) {
                janeKey = (String) row.get("key");
                break;
            }
        }
        assertThat(janeKey).isNotNull();

        ResponseEntity<List> janeBooks = restTemplate.getForEntity("/api/browse/authors/{authorKey}/books", List.class, janeKey);
        assertThat(janeBooks.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(janeBooks.getBody()).hasSize(2);
    }

    @Test
    void recentBrowseAndReportsReturnStructuredData() {
        UUID firstShelfBook = insertBook("First Added", "Zulu Author", LocalDate.of(2026, 3, 18), "shelf-a", "general");
        UUID shelfAlpha = insertBook("Alpha Shelf", "Alpha Author", LocalDate.of(2026, 3, 19), "shelf-a", "general");
        UUID shelfBeta = insertBook("Beta Shelf", "Beta Author", LocalDate.of(2026, 3, 20), "shelf-a", "general");
        UUID secondId = insertBook("Second Added", "Author Two", LocalDate.of(2026, 3, 22), null, "bestsellers");
        jdbcTemplate.update("update book set updated_at = now() where id = :id", Map.of("id", secondId));
        jdbcTemplate.update("update book set shelf_coords = :coords where id = :id", Map.of("coords", "b2", "id", firstShelfBook));
        jdbcTemplate.update("update book set shelf_coords = :coords where id = :id", Map.of("coords", "a2", "id", shelfAlpha));
        jdbcTemplate.update("update book set shelf_coords = :coords where id = :id", Map.of("coords", "a10", "id", shelfBeta));

        ResponseEntity<List> recentlyAdded = restTemplate.getForEntity("/api/browse/recently-added?limit=2", List.class);
        assertThat(recentlyAdded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) recentlyAdded.getBody().getFirst()).get("title")).isEqualTo("Second Added");

        ResponseEntity<List> recentlyUpdated = restTemplate.getForEntity("/api/browse/recently-updated?limit=1", List.class);
        assertThat(recentlyUpdated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) recentlyUpdated.getBody().getFirst()).get("title")).isEqualTo("Second Added");

        ResponseEntity<List> collectionReport = restTemplate.getForEntity("/api/reports/collections", List.class);
        assertThat(collectionReport.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(collectionReport.getBody()).extracting(item -> ((Map<?, ?>) item).get("name"))
                .contains("General", "Bestsellers");

        ResponseEntity<List> shelfReport = restTemplate.getForEntity("/api/reports/shelves", List.class);
        assertThat(shelfReport.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shelfReport.getBody()).anySatisfy(item ->
                assertThat(((Map<?, ?>) item).get("id")).isEqualTo("shelf-a"));
        assertThat(shelfReport.getBody()).extracting(item -> ((Map<?, ?>) item).get("name"))
                .contains("Unassigned");
        Map<String, Object> shelfA = null;
        for (Object item : shelfReport.getBody()) {
            Map<String, Object> row = (Map<String, Object>) item;
            if ("shelf-a".equals(row.get("id"))) {
                shelfA = row;
                break;
            }
        }
        assertThat(shelfA).isNotNull();
        List<Map<String, Object>> shelfBooks = (List<Map<String, Object>>) shelfA.get("books");
        assertThat(shelfBooks).extracting(item -> item.get("title"))
                .containsExactly("Alpha Shelf", "Beta Shelf", "First Added");
        assertThat(shelfBooks).extracting(item -> item.get("shelfCoords"))
                .containsExactly("a2", "a10", "b2");

        ResponseEntity<Map> missingLocation = restTemplate.getForEntity("/api/reports/missing-location", Map.class);
        assertThat(missingLocation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) missingLocation.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) missingLocation.getBody().get("books")).getFirst())
                .containsEntry("title", "Second Added");
    }

    @Test
    void valueByCollectionReportsUseTheirRespectiveFallbackOrders() {
        UUID generalPaidFirst = insertBook("General Paid First", "Paid Author", LocalDate.of(2026, 3, 18), "shelf-a", "general");
        UUID generalReplacementOnly = insertBook("General Replacement Only", "Replacement Author", LocalDate.of(2026, 3, 19), "shelf-a", "general");
        UUID excludedAndGeneral = insertBook("General And Excluded", "Excluded Author", LocalDate.of(2026, 3, 20), "shelf-a", "general");

        jdbcTemplate.update("update book set price_paid = :paid, price_on_item = :printed, price_to_replace = :replacement where id = :id",
                Map.of("paid", BigDecimal.valueOf(10), "printed", BigDecimal.valueOf(12), "replacement", BigDecimal.valueOf(15), "id", generalPaidFirst));
        jdbcTemplate.update("update book set price_paid = 0, price_on_item = 0, price_to_replace = :replacement where id = :id",
                Map.of("replacement", BigDecimal.valueOf(20), "id", generalReplacementOnly));
        jdbcTemplate.update("update book set price_paid = :paid, price_on_item = :printed, price_to_replace = :replacement where id = :id",
                Map.of("paid", BigDecimal.valueOf(30), "printed", BigDecimal.valueOf(40), "replacement", BigDecimal.valueOf(50), "id", excludedAndGeneral));
        jdbcTemplate.update("insert into book_collection (book_id, collection_id) values (:bookId, 'excluded')",
                Map.of("bookId", excludedAndGeneral));

        ResponseEntity<List> paidReport = restTemplate.getForEntity("/api/reports/value-by-collection-paid", List.class);
        assertThat(paidReport.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paidReport.getBody()).extracting(item -> ((Map<?, ?>) item).get("id"))
                .startsWith("excluded", "general");
        Map<String, Object> generalPaidRow = findById(paidReport.getBody(), "general");
        Map<String, Object> excludedPaidRow = findById(paidReport.getBody(), "excluded");
        assertThat(((Number) generalPaidRow.get("bookCount")).intValue()).isEqualTo(3);
        assertThat((String) generalPaidRow.get("name")).isEqualTo("General");
        assertThat(new BigDecimal(String.valueOf(generalPaidRow.get("totalValue")))).isEqualByComparingTo("30");
        assertThat(new BigDecimal(String.valueOf(excludedPaidRow.get("totalValue")))).isEqualByComparingTo("30");

        ResponseEntity<List> replacementReport = restTemplate.getForEntity("/api/reports/value-by-collection-replacement", List.class);
        assertThat(replacementReport.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replacementReport.getBody()).extracting(item -> ((Map<?, ?>) item).get("id"))
                .startsWith("excluded", "general");
        Map<String, Object> generalReplacementRow = findById(replacementReport.getBody(), "general");
        Map<String, Object> excludedReplacementRow = findById(replacementReport.getBody(), "excluded");
        assertThat(new BigDecimal(String.valueOf(generalReplacementRow.get("totalValue")))).isEqualByComparingTo("35");
        assertThat(new BigDecimal(String.valueOf(excludedReplacementRow.get("totalValue")))).isEqualByComparingTo("50");
    }

    private UUID insertBook(String title, String author, LocalDate dateAdded, String shelfId, String collectionId) {
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
        params.put("special", false);
        params.put("hardback", false);
        params.put("vols", 1);
        params.put("series", "Test Series");
        params.put("shelfCoords", "A-1");
        params.put("note", "test note");
        params.put("pricePaid", BigDecimal.valueOf(10.00));
        params.put("priceOnItem", BigDecimal.valueOf(12.00));
        params.put("priceToReplace", BigDecimal.valueOf(15.00));
        params.put("priceToReplaceChecked", LocalDate.of(2026, 3, 21));
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

    private Map<String, Object> findById(List<?> rows, String id) {
        for (Object item : rows) {
            Map<String, Object> typedRow = (Map<String, Object>) item;
            if (id.equals(typedRow.get("id"))) {
                return typedRow;
            }
        }
        throw new IllegalStateException("Could not find row with id " + id);
    }
}
