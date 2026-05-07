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

class BookApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createBookPersistsBookAndCollections() {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", "The Civil War"),
                Map.entry("author", "Shelby Foote"),
                Map.entry("publicationCity", "New York"),
                Map.entry("publisher", "Random House"),
                Map.entry("imprint", "Vintage"),
                Map.entry("year", 1958),
                Map.entry("firstPublished", 1958),
                Map.entry("dateAdded", "2026-03-21"),
                Map.entry("lastAudited", "2026-03-25"),
                Map.entry("shelfId", "shelf-a"),
                Map.entry("isbn", "9780394749136"),
                Map.entry("special", true),
                Map.entry("digital", true),
                Map.entry("hardback", false),
                Map.entry("dustjacket", true),
                Map.entry("slipcase", false),
                Map.entry("video", true),
                Map.entry("vols", 3),
                Map.entry("note", "Integration test note"),
                Map.entry("pricePaid", 18.50),
                Map.entry("collectionIds", List.of("general", "secondary"))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/books", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "The Civil War");
        assertThat(response.getBody()).containsEntry("author", "Shelby Foote");
        assertThat(response.getBody()).containsEntry("publicationCity", "New York");
        assertThat(response.getBody()).containsEntry("publisher", "Random House");
        assertThat(response.getBody()).containsEntry("imprint", "Vintage");
        assertThat(response.getBody()).containsEntry("firstPublished", 1958);
        assertThat(response.getBody()).containsEntry("shelfId", "shelf-a");
        assertThat(response.getBody()).containsEntry("lastAudited", "2026-03-25");
        assertThat(response.getBody()).containsEntry("digital", true);
        assertThat(response.getBody()).containsEntry("dustjacket", true);
        assertThat(response.getBody()).containsEntry("video", true);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKeys("createdAt", "updatedAt");
        assertThat((List<String>) response.getBody().get("collectionIds"))
                .containsExactlyInAnyOrder("general", "secondary");

        UUID bookId = UUID.fromString((String) response.getBody().get("id"));

        Integer bookCount = jdbcTemplate.queryForObject(
                "select count(*) from book where id = :id and title = :title and imprint = :imprint and first_published = :firstPublished and special = true and vols = 3 and last_audited = :lastAudited",
                Map.of("id", bookId, "title", "The Civil War", "imprint", "Vintage", "firstPublished", 1958, "lastAudited", LocalDate.of(2026, 3, 25)),
                Integer.class);
        assertThat(bookCount).isEqualTo(1);

        Integer collectionCount = jdbcTemplate.queryForObject(
                "select count(*) from book_collection where book_id = :bookId",
                Map.of("bookId", bookId),
                Integer.class);
        assertThat(collectionCount).isEqualTo(2);
    }

    @Test
    void createBookTrimsUserProvidedStrings() {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", "  The Civil War  "),
                Map.entry("author", "  Shelby Foote  "),
                Map.entry("publicationCity", "  New York  "),
                Map.entry("publisher", "  Random House  "),
                Map.entry("imprint", "  Vintage  "),
                Map.entry("edition", "  First  "),
                Map.entry("frontpages", "  xii  "),
                Map.entry("shelfId", "  shelf-a  "),
                Map.entry("authorSecondary", "  Someone Else  "),
                Map.entry("isbn", "  9780394749136  "),
                Map.entry("series", "  Trilogy  "),
                Map.entry("shelfCoords", "  A-1  "),
                Map.entry("note", "  Integration test note  "),
                Map.entry("special", true),
                Map.entry("digital", true),
                Map.entry("hardback", false),
                Map.entry("dustjacket", true),
                Map.entry("slipcase", false),
                Map.entry("video", true),
                Map.entry("vols", 3),
                Map.entry("collectionIds", List.of(" general ", " secondary "))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/books", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "The Civil War");
        assertThat(response.getBody()).containsEntry("author", "Shelby Foote");
        assertThat(response.getBody()).containsEntry("publicationCity", "New York");
        assertThat(response.getBody()).containsEntry("publisher", "Random House");
        assertThat(response.getBody()).containsEntry("imprint", "Vintage");
        assertThat(response.getBody()).containsEntry("edition", "First");
        assertThat(response.getBody()).containsEntry("frontpages", "xii");
        assertThat(response.getBody()).containsEntry("shelfId", "shelf-a");
        assertThat(response.getBody()).containsEntry("authorSecondary", "Someone Else");
        assertThat(response.getBody()).containsEntry("isbn", "9780394749136");
        assertThat(response.getBody()).containsEntry("series", "Trilogy");
        assertThat(response.getBody()).containsEntry("shelfCoords", "A-1");
        assertThat(response.getBody()).containsEntry("note", "Integration test note");
        assertThat(response.getBody()).containsEntry("digital", true);
        assertThat(response.getBody()).containsEntry("dustjacket", true);
        assertThat(response.getBody()).containsEntry("video", true);
        assertThat((List<String>) response.getBody().get("collectionIds"))
                .containsExactlyInAnyOrder("general", "secondary");
    }

    @Test
    void updateBookReplacesCollectionAssignments() {
        UUID bookId = insertBook("Original Title", "general");

        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", "Updated Title"),
                Map.entry("author", "Updated Author"),
                Map.entry("publicationCity", "Boston"),
                Map.entry("publisher", "Updated Publisher"),
                Map.entry("imprint", "Updated Imprint"),
                Map.entry("year", 2024),
                Map.entry("firstPublished", 1999),
                Map.entry("dateAdded", "2026-03-20"),
                Map.entry("lastAudited", "2026-03-23"),
                Map.entry("shelfId", "shelf-b"),
                Map.entry("isbn", "1111111111"),
                Map.entry("special", false),
                Map.entry("digital", true),
                Map.entry("hardback", true),
                Map.entry("dustjacket", true),
                Map.entry("slipcase", true),
                Map.entry("video", false),
                Map.entry("vols", 1),
                Map.entry("collectionIds", List.of("bestsellers", "language"))
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/books/" + bookId,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "Updated Title");
        assertThat(response.getBody()).containsEntry("author", "Updated Author");
        assertThat(response.getBody()).containsEntry("publicationCity", "Boston");
        assertThat(response.getBody()).containsEntry("publisher", "Updated Publisher");
        assertThat(response.getBody()).containsEntry("imprint", "Updated Imprint");
        assertThat(response.getBody()).containsEntry("firstPublished", 1999);
        assertThat(response.getBody()).containsEntry("shelfId", "shelf-b");
        assertThat(response.getBody()).containsEntry("lastAudited", "2026-03-23");
        assertThat(response.getBody()).containsEntry("digital", true);
        assertThat(response.getBody()).containsEntry("dustjacket", true);
        assertThat(response.getBody()).containsEntry("slipcase", true);
        assertThat((List<String>) response.getBody().get("collectionIds"))
                .containsExactlyInAnyOrder("bestsellers", "language");

        Integer oldCollectionCount = jdbcTemplate.queryForObject(
                "select count(*) from book_collection where book_id = :bookId and collection_id = 'general'",
                Map.of("bookId", bookId),
                Integer.class);
        assertThat(oldCollectionCount).isZero();

        Integer replacementCount = jdbcTemplate.queryForObject(
                "select count(*) from book_collection where book_id = :bookId",
                Map.of("bookId", bookId),
                Integer.class);
        assertThat(replacementCount).isEqualTo(2);
    }

    @Test
    void listBooksSupportsCollectionAndShelfFilters() {
        UUID generalBookId = insertBook("Archive Survey", "general");
        UUID bestsellerBookId = insertBook("Close-Up Bestsellers", "bestsellers");

        jdbcTemplate.update("update book set shelf_id = 'shelf-a' where id = :id", Map.of("id", generalBookId));
        jdbcTemplate.update("update book set shelf_id = 'shelf-b' where id = :id", Map.of("id", bestsellerBookId));

        ResponseEntity<Map> filtered = restTemplate.getForEntity(
                "/api/books?collectionId=general&shelfId=shelf-a",
                Map.class);

        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filtered.getBody()).isNotNull();
        assertThat(((Number) filtered.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) filtered.getBody().get("items"))).hasSize(1);
        assertThat(((List<Map<String, Object>>) filtered.getBody().get("items")).getFirst()).containsEntry("title", "Archive Survey");
    }

    @Test
    void listBooksReturnsPaginationMetadata() {
        insertBook("Alpha", "general");
        insertBook("Beta", "general");

        ResponseEntity<Map> response = restTemplate.getForEntity("/api/books?size=1&page=0", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("page", 0);
        assertThat(response.getBody()).containsEntry("size", 1);
        assertThat(((Number) response.getBody().get("totalItems")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(((Number) response.getBody().get("totalPages")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat((List<?>) response.getBody().get("items")).hasSize(1);
    }

    @Test
    void listBooksSupportsSortAndExtendedFilters() {
        UUID shelfBookId = insertBook("Alpha Title", "general");
        UUID noShelfBookId = insertBook("Zulu Title", "general");
        UUID digitalBookId = insertBook("Digital Title", "general");
        UUID hardbackBookId = insertBook("Gamma Title", "general");
        UUID dustjacketBookId = insertBook("Dustjacket Title", "general");

        jdbcTemplate.update("""
                        update book
                        set shelf_id = 'shelf-a',
                            special = true,
                            hardback = false
                        where id = :id
                        """,
                Map.of("id", shelfBookId));
        jdbcTemplate.update("""
                        update book
                        set shelf_id = null,
                            special = false,
                            digital = false,
                            hardback = false
                        where id = :id
                        """,
                Map.of("id", noShelfBookId));
        jdbcTemplate.update("""
                        update book
                        set shelf_id = 'shelf-c',
                            special = false,
                            digital = true,
                            hardback = false
                        where id = :id
                        """,
                Map.of("id", digitalBookId));
        jdbcTemplate.update("""
                        update book
                        set shelf_id = 'shelf-b',
                            special = false,
                            digital = false,
                            hardback = true
                        where id = :id
                        """,
                Map.of("id", hardbackBookId));
        jdbcTemplate.update("""
                        update book
                        set shelf_id = 'shelf-d',
                            dustjacket = true,
                            slipcase = true,
                            video = true
                        where id = :id
                        """,
                Map.of("id", dustjacketBookId));

        ResponseEntity<Map> noShelfResponse = restTemplate.getForEntity(
                "/api/books?hasShelf=false&sort=title&direction=asc",
                Map.class);
        assertThat(noShelfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) noShelfResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) noShelfResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Zulu Title");

        ResponseEntity<Map> specialResponse = restTemplate.getForEntity(
                "/api/books?specialOnly=true",
                Map.class);
        assertThat(specialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) specialResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) specialResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Alpha Title");

        ResponseEntity<Map> digitalResponse = restTemplate.getForEntity(
                "/api/books?digitalOnly=true",
                Map.class);
        assertThat(digitalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) digitalResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) digitalResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Digital Title");

        ResponseEntity<Map> hardbackResponse = restTemplate.getForEntity(
                "/api/books?hardbackOnly=true",
                Map.class);
        assertThat(hardbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) hardbackResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) hardbackResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Gamma Title");

        ResponseEntity<Map> dustjacketResponse = restTemplate.getForEntity(
                "/api/books?dustjacketOnly=true",
                Map.class);
        assertThat(dustjacketResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) dustjacketResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) dustjacketResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Dustjacket Title");

        ResponseEntity<Map> slipcaseVideoResponse = restTemplate.getForEntity(
                "/api/books?slipcaseOnly=true&videoOnly=true&sort=title&direction=asc",
                Map.class);
        assertThat(slipcaseVideoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) slipcaseVideoResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) slipcaseVideoResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Dustjacket Title");

        ResponseEntity<Map> inclusiveFlagsResponse = restTemplate.getForEntity(
                "/api/books?digitalOnly=true&hardbackOnly=true&sort=title&direction=asc",
                Map.class);
        assertThat(inclusiveFlagsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) inclusiveFlagsResponse.getBody().get("totalItems")).intValue()).isEqualTo(2);
        assertThat(((List<Map<String, Object>>) inclusiveFlagsResponse.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("Digital Title", "Gamma Title");

        ResponseEntity<Map> sortedResponse = restTemplate.getForEntity(
                "/api/books?sort=title&direction=desc&size=2&page=0",
                Map.class);
        assertThat(sortedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) sortedResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Zulu Title");
    }

    @Test
    void listBooksSupportsLastAuditedBeforeAndAfterFilters() {
        UUID olderBookId = insertBook("Older Audit", "general");
        UUID newerBookId = insertBook("Newer Audit", "general");
        UUID unauditedBookId = insertBook("Never Audited", "general");

        jdbcTemplate.update("update book set last_audited = :lastAudited where id = :id",
                Map.of("id", olderBookId, "lastAudited", LocalDate.of(2026, 3, 10)));
        jdbcTemplate.update("update book set last_audited = :lastAudited where id = :id",
                Map.of("id", newerBookId, "lastAudited", LocalDate.of(2026, 3, 20)));
        jdbcTemplate.update("update book set last_audited = null where id = :id",
                Map.of("id", unauditedBookId));

        ResponseEntity<Map> beforeResponse = restTemplate.getForEntity(
                "/api/books?lastAuditedMode=before&lastAuditedOn=2026-03-15&sort=title&direction=asc",
                Map.class);
        assertThat(beforeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) beforeResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) beforeResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Older Audit");

        ResponseEntity<Map> afterResponse = restTemplate.getForEntity(
                "/api/books?lastAuditedMode=after&lastAuditedOn=2026-03-15&sort=title&direction=asc",
                Map.class);
        assertThat(afterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) afterResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) afterResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Newer Audit");

        ResponseEntity<Map> neverResponse = restTemplate.getForEntity(
                "/api/books?lastAuditedMode=never&sort=title&direction=asc",
                Map.class);
        assertThat(neverResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) neverResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) neverResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Never Audited");
    }

    @Test
    void listBooksSupportsDateAddedBeforeAndAfterFilters() {
        UUID olderBookId = insertBook("Older Added", "general");
        UUID newerBookId = insertBook("Newer Added", "general");
        UUID undatedBookId = insertBook("No Added Date", "general");

        jdbcTemplate.update("update book set date_added = :dateAdded where id = :id",
                Map.of("id", olderBookId, "dateAdded", LocalDate.of(2026, 3, 10)));
        jdbcTemplate.update("update book set date_added = :dateAdded where id = :id",
                Map.of("id", newerBookId, "dateAdded", LocalDate.of(2026, 3, 20)));
        jdbcTemplate.update("update book set date_added = null where id = :id",
                Map.of("id", undatedBookId));

        ResponseEntity<Map> beforeResponse = restTemplate.getForEntity(
                "/api/books?dateAddedMode=before&dateAddedOn=2026-03-15&sort=title&direction=asc",
                Map.class);
        assertThat(beforeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) beforeResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) beforeResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Older Added");

        ResponseEntity<Map> afterResponse = restTemplate.getForEntity(
                "/api/books?dateAddedMode=after&dateAddedOn=2026-03-15&sort=title&direction=asc",
                Map.class);
        ResponseEntity<Map> yearResponse = restTemplate.getForEntity(
                "/api/books?dateAddedMode=year&dateAddedYear=2026&sort=title&direction=asc",
                Map.class);
        assertThat(afterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) afterResponse.getBody().get("totalItems")).intValue()).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) afterResponse.getBody().get("items")).getFirst())
                .containsEntry("title", "Newer Added");
        assertThat(yearResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) yearResponse.getBody().get("totalItems")).intValue()).isEqualTo(2);
        assertThat(((List<Map<String, Object>>) yearResponse.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("Newer Added", "Older Added");
    }

    @Test
    void listBooksSupportsNoBestEstimateFilterForNavigationWorkingSet() {
        UUID nullValueBookId = insertBook("Alpha No Value", "general");
        UUID zeroValueBookId = insertBook("Beta Zero Value", "general");
        UUID paidValueBookId = insertBook("Gamma Paid Value", "general");

        jdbcTemplate.update("""
                        update book
                        set price_paid = null,
                            price_on_item = null,
                            price_to_replace = null
                        where id = :id
                        """,
                Map.of("id", nullValueBookId));
        jdbcTemplate.update("""
                        update book
                        set price_paid = 0,
                            price_on_item = 0,
                            price_to_replace = 0
                        where id = :id
                        """,
                Map.of("id", zeroValueBookId));
        jdbcTemplate.update("""
                        update book
                        set price_paid = 25.00,
                            price_on_item = 0,
                            price_to_replace = 0
                        where id = :id
                        """,
                Map.of("id", paidValueBookId));

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/books?noBestEstimateOnly=true&sort=title&direction=asc",
                Map.class);
        ResponseEntity<Map> navigation = restTemplate.getForEntity(
                "/api/books/%s/navigation?noBestEstimateOnly=true&sort=title&direction=asc".formatted(zeroValueBookId),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) response.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("Alpha No Value", "Beta Zero Value");

        assertThat(navigation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(navigation.getBody())
                .containsEntry("previousBookId", nullValueBookId.toString())
                .containsEntry("nextBookId", null)
                .containsEntry("position", 2)
                .containsEntry("totalItems", 2);
    }

    @Test
    void bookNavigationReturnsNeighborsAcrossTheFullFilteredResultSet() {
        UUID firstBookId = insertBook("Alpha Title", "general");
        UUID secondBookId = insertBook("Beta Title", "general");
        UUID thirdBookId = insertBook("Gamma Title", "general");

        jdbcTemplate.update("update book set author = :author, year = :year where id = :id",
                Map.of("id", firstBookId, "author", "Author A", "year", 2020));
        jdbcTemplate.update("update book set author = :author, year = :year, digital = true where id = :id",
                Map.of("id", secondBookId, "author", "Author B", "year", 2021));
        jdbcTemplate.update("update book set author = :author, year = :year where id = :id",
                Map.of("id", thirdBookId, "author", "Author C", "year", 2022));

        ResponseEntity<Map> navigation = restTemplate.getForEntity(
                "/api/books/%s/navigation?collectionIds=general&sort=title&direction=asc".formatted(secondBookId),
                Map.class);

        assertThat(navigation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(navigation.getBody())
                .containsEntry("previousBookId", firstBookId.toString())
                .containsEntry("nextBookId", thirdBookId.toString())
                .containsEntry("position", 2)
                .containsEntry("totalItems", 3);
    }

    @Test
    void listBooksSupportsMultiCollectionOrAndAndNoCollectionFilters() {
        UUID generalOnlyId = insertBook("General Only", "general");
        UUID secondaryOnlyId = insertBook("Secondary Only", "secondary");
        UUID bothId = insertBook("Both Collections", "general");
        UUID noCollectionId = insertBook("No Collection Book", "general");

        jdbcTemplate.update("insert into book_collection (book_id, collection_id) values (:bookId, :collectionId)",
                Map.of("bookId", bothId, "collectionId", "secondary"));
        jdbcTemplate.update("delete from book_collection where book_id = :bookId",
                Map.of("bookId", noCollectionId));

        ResponseEntity<Map> generalResponse = restTemplate.getForEntity(
                "/api/books?collectionIds=general&sort=title&direction=asc",
                Map.class);
        ResponseEntity<Map> orResponse = restTemplate.getForEntity(
                "/api/books?collectionIds=general,secondary&collectionMode=or&sort=title&direction=asc",
                Map.class);
        ResponseEntity<Map> andResponse = restTemplate.getForEntity(
                "/api/books?collectionIds=general,secondary&collectionMode=and&sort=title&direction=asc",
                Map.class);
        ResponseEntity<Map> noCollectionResponse = restTemplate.getForEntity(
                "/api/books?noCollectionOnly=true&sort=title&direction=asc",
                Map.class);

        assertThat(generalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) generalResponse.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("Both Collections", "General Only");

        assertThat(orResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) orResponse.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("Both Collections", "General Only", "Secondary Only");

        assertThat(andResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) andResponse.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("Both Collections");

        assertThat(noCollectionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((List<Map<String, Object>>) noCollectionResponse.getBody().get("items")))
                .extracting(item -> item.get("title"))
                .containsExactly("No Collection Book");
    }

    @Test
    void createBookRejectsUnknownShelf() {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", "Bad Shelf Book"),
                Map.entry("shelfId", "doesNotExist"),
                Map.entry("collectionIds", List.of("general"))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/books", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "Invalid reference");
        assertThat((String) response.getBody().get("detail")).contains("Unknown shelf id");
    }

    @Test
    void deleteBookRemovesBookAndAssociations() {
        UUID bookId = insertBook("Delete Me", "general");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/books/" + bookId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer bookCount = jdbcTemplate.queryForObject(
                "select count(*) from book where id = :id",
                Map.of("id", bookId),
                Integer.class);
        Integer collectionCount = jdbcTemplate.queryForObject(
                "select count(*) from book_collection where book_id = :bookId",
                Map.of("bookId", bookId),
                Integer.class);

        assertThat(bookCount).isZero();
        assertThat(collectionCount).isZero();

        ResponseEntity<Map> missing = restTemplate.getForEntity("/api/books/" + bookId, Map.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBookRejectsValidationErrors() {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", ""),
                Map.entry("pages", -10),
                Map.entry("collectionIds", List.of("general"))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/books", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("title", "Validation error");
        assertThat((List<String>) response.getBody().get("errors"))
                .anyMatch(message -> message.contains("title"))
                .anyMatch(message -> message.contains("pages"));
    }

    @Test
    void searchMatchesUnicodeAuthorNames() {
        insertBook("Lingua Latina", "general");
        jdbcTemplate.update(
                "update book set author = :author where title = :title",
                Map.of("author", "Ørberg, Hans H.", "title", "Lingua Latina"));

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/books?q={q}&sort=title&direction=asc",
                Map.class,
                "Ørberg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("totalItems")).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) response.getBody().get("items")).get(0).get("author"))
                .isEqualTo("Ørberg, Hans H.");
    }

    @Test
    void searchMatchesSecondaryAuthorAndSeries() {
        UUID secondaryAuthorBookId = insertBook("Primary Title", "general");
        UUID seriesBookId = insertBook("Series Title", "general");
        jdbcTemplate.update(
                "update book set author_secondary = :authorSecondary where id = :id",
                Map.of("authorSecondary", "Hidden Collaborator", "id", secondaryAuthorBookId));
        jdbcTemplate.update(
                "update book set series = :series where id = :id",
                Map.of("series", "Invisible Cycle", "id", seriesBookId));

        ResponseEntity<Map> secondaryAuthorResponse = restTemplate.getForEntity(
                "/api/books?q={q}&sort=title&direction=asc",
                Map.class,
                "Collaborator");
        ResponseEntity<Map> seriesResponse = restTemplate.getForEntity(
                "/api/books?q={q}&sort=title&direction=asc",
                Map.class,
                "Invisible");

        assertThat(secondaryAuthorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondaryAuthorResponse.getBody()).isNotNull();
        assertThat(secondaryAuthorResponse.getBody().get("totalItems")).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) secondaryAuthorResponse.getBody().get("items")).get(0).get("title"))
                .isEqualTo("Primary Title");
        assertThat(seriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(seriesResponse.getBody()).isNotNull();
        assertThat(seriesResponse.getBody().get("totalItems")).isEqualTo(1);
        assertThat(((List<Map<String, Object>>) seriesResponse.getBody().get("items")).get(0).get("title"))
                .isEqualTo("Series Title");
    }

    private UUID insertBook(String title, String collectionId) {
        var params = new java.util.HashMap<String, Object>();
        params.put("title", title);
        params.put("author", "Test Author");
        params.put("publicationCity", null);
        params.put("publisher", "Test Publisher");
        params.put("year", 2020);
        params.put("edition", "1");
        params.put("pages", 250);
        params.put("frontpages", "xii");
        params.put("dateAdded", LocalDate.of(2026, 3, 21));
        params.put("shelfId", "shelf-a");
        params.put("lastAudited", LocalDate.of(2026, 3, 22));
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
                            last_audited, author_secondary, isbn, special, hardback, vols, series, shelf_coords, note,
                            price_paid, price_on_item, price_to_replace, price_to_replace_checked
                        ) values (
                            :title, :author, :publicationCity, :publisher, :year, :edition, :pages, :frontpages, :dateAdded, :shelfId,
                            :lastAudited, :authorSecondary, :isbn, :special, :hardback, :vols, :series, :shelfCoords, :note,
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
