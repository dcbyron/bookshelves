package com.baroquepotion.bookshelves;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listCollectionsIncludesUsageCounts() {
        insertBook("Usage Count Collection Book", "general");

        ResponseEntity<List> response = restTemplate.getForEntity("/api/collections", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> general = findById(response.getBody(), "general");
        assertThat(((Number) general.get("usageCount")).intValue()).isEqualTo(1);
    }

    @Test
    void listShelvesIncludesUsageCounts() {
        UUID bookId = insertBook("Usage Count Shelf Book", "general");
        jdbcTemplate.update("update book set shelf_id = 'shelf-a' where id = :id", Map.of("id", bookId));

        ResponseEntity<List> response = restTemplate.getForEntity("/api/shelves", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> shelfA = findById(response.getBody(), "shelf-a");
        assertThat(shelfA).containsEntry("audited", false);
        assertThat(((Number) shelfA.get("usageCount")).intValue()).isEqualTo(1);
    }

    @Test
    void coordinateAuditsListDistinctOccupiedCoordinatesAndPersistAuditState() {
        UUID first = insertBook("Coordinate Audit First", "general");
        UUID duplicate = insertBook("Coordinate Audit Duplicate", "general");
        UUID second = insertBook("Coordinate Audit Second", "general");
        UUID blank = insertBook("Coordinate Audit Blank", "general");
        UUID missing = insertBook("Coordinate Audit Missing", "general");
        jdbcTemplate.update("update book set shelf_coords = 'a1' where id = :id", Map.of("id", first));
        jdbcTemplate.update("update book set shelf_coords = 'a1' where id = :id", Map.of("id", duplicate));
        jdbcTemplate.update("update book set shelf_coords = 'b2' where id = :id", Map.of("id", second));
        jdbcTemplate.update("update book set shelf_coords = ' ' where id = :id", Map.of("id", blank));
        jdbcTemplate.update("update book set shelf_coords = null where id = :id", Map.of("id", missing));

        ResponseEntity<List> initial = restTemplate.getForEntity("/api/shelf-coordinate-audits", List.class);

        assertThat(initial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initial.getBody()).isNotNull();
        assertThat(initial.getBody()).hasSize(2);
        Map<String, Object> a1 = findCoordinateAudit(initial.getBody(), "shelf-a", "a1");
        assertThat(a1).containsEntry("shelfName", "General shelf");
        assertThat(a1).containsEntry("audited", false);

        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/shelf-coordinate-audits/shelf-a/a1",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("audited", true)),
                Map.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("audited", true);
        ResponseEntity<List> afterUpdate = restTemplate.getForEntity("/api/shelf-coordinate-audits", List.class);
        assertThat(findCoordinateAudit(afterUpdate.getBody(), "shelf-a", "a1")).containsEntry("audited", true);
    }

    @Test
    void deleteCollectionRejectsInUseCollection() {
        insertBook("Protected Collection Book", "general");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/collections/general",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "Resource in use");
        assertThat((String) response.getBody().get("detail")).contains("cannot be deleted");
    }

    @Test
    void deleteShelfRejectsInUseShelf() {
        UUID bookId = insertBook("Protected Shelf Book", "general");
        jdbcTemplate.update("update book set shelf_id = 'shelf-a' where id = :id", Map.of("id", bookId));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/shelves/shelf-a",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "Resource in use");
        assertThat((String) response.getBody().get("detail")).contains("cannot be deleted");
    }

    @Test
    void createCollectionTrimsUserProvidedStrings() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/collections",
                Map.of("id", " trimmed-id ", "name", " Trimmed Name "),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("id", "trimmed-id");
        assertThat(response.getBody()).containsEntry("name", "Trimmed Name");
    }

    @Test
    void createShelfTrimsUserProvidedStrings() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/shelves",
                Map.of("id", " trimmed-shelf ", "name", " Trimmed Shelf ", "audited", true),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("id", "trimmed-shelf");
        assertThat(response.getBody()).containsEntry("name", "Trimmed Shelf");
        assertThat(response.getBody()).containsEntry("audited", true);
    }

    @Test
    void listPublishersIncludesUsageCounts() {
        insertBook("Publisher One", "P&R", "general");
        insertBook("Publisher Two", "P&R", "bestsellers");
        insertBook("Publisher Three", "Presbyterian & Reformed", "general");

        ResponseEntity<List> response = restTemplate.getForEntity("/api/publishers", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(item -> ((Map<?, ?>) item).get("publisher"))
                .contains("P&R", "Presbyterian & Reformed");
        Map<String, Object> pr = findByPublisher(response.getBody(), "P&R");
        assertThat(((Number) pr.get("bookCount")).intValue()).isEqualTo(2);
    }

    @Test
    void renamePublisherBulkUpdatesMatchingBooks() {
        insertBook("Publisher One", "P&R", "general");
        insertBook("Publisher Two", "P&R", "bestsellers");
        insertBook("Publisher Three", "Presbyterian & Reformed", "general");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/publishers/rename",
                Map.of("sourcePublisher", " P&R ", "targetPublisher", " Presbyterian & Reformed "),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("publisher", "Presbyterian & Reformed");
        assertThat(((Number) response.getBody().get("updatedCount")).intValue()).isEqualTo(2);

        ResponseEntity<List> publishers = restTemplate.getForEntity("/api/publishers", List.class);
        assertThat(publishers.getBody()).isNotNull();
        assertThat(publishers.getBody()).extracting(item -> ((Map<?, ?>) item).get("publisher"))
                .containsExactly("Presbyterian & Reformed");
        Map<String, Object> canonical = findByPublisher(publishers.getBody(), "Presbyterian & Reformed");
        assertThat(((Number) canonical.get("bookCount")).intValue()).isEqualTo(3);
    }

    private UUID insertBook(String title, String publisher, String collectionId) {
        var params = new java.util.HashMap<String, Object>();
        params.put("title", title);
        params.put("author", "Test Author");
        params.put("publicationCity", null);
        params.put("publisher", publisher);
        params.put("year", 2020);
        params.put("edition", "1");
        params.put("pages", 250);
        params.put("frontpages", "xii");
        params.put("dateAdded", LocalDate.of(2026, 3, 21));
        params.put("shelfId", "shelf-a");
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

    private UUID insertBook(String title, String collectionId) {
        return insertBook(title, "Test Publisher", collectionId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findById(List<?> rows, String id) {
        for (Object row : rows) {
            Map<String, Object> typedRow = (Map<String, Object>) row;
            if (id.equals(typedRow.get("id"))) {
                return typedRow;
            }
        }
        throw new IllegalStateException("Could not find row with id " + id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findCoordinateAudit(List<?> rows, String shelfId, String shelfCoords) {
        for (Object row : rows) {
            Map<String, Object> typedRow = (Map<String, Object>) row;
            if (shelfId.equals(typedRow.get("shelfId")) && shelfCoords.equals(typedRow.get("shelfCoords"))) {
                return typedRow;
            }
        }
        throw new IllegalStateException("Could not find coordinate audit row for " + shelfId + " " + shelfCoords);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findByPublisher(List<?> rows, String publisher) {
        for (Object row : rows) {
            Map<String, Object> typedRow = (Map<String, Object>) row;
            if (publisher.equals(typedRow.get("publisher"))) {
                return typedRow;
            }
        }
        throw new IllegalStateException("Could not find row with publisher " + publisher);
    }
}
