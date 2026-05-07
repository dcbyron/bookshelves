package com.baroquepotion.bookshelves.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Filter and paging values used when searching the catalog.
 *
 * <p>The Books workflow now supports richer collection scoping than the
 * original single-collection filter. {@code collectionIds} may contain zero,
 * one, or many selected collection ids; {@code collectionMode} controls how
 * multiple selections combine; {@code noCollectionOnly} provides the
 * special "books with no collection assignments" path used by the Books view;
 * and {@code noBestEstimateOnly} narrows the editable working set to books
 * without any non-zero price basis.
 */
public record BookSearchCriteria(
        String query,
        List<String> collectionIds,
        String collectionMode,
        Boolean noCollectionOnly,
        Boolean noBestEstimateOnly,
        String shelfId,
        Integer year,
        Boolean hasShelf,
        Boolean specialOnly,
        Boolean digitalOnly,
        Boolean hardbackOnly,
        Boolean dustjacketOnly,
        Boolean slipcaseOnly,
        Boolean videoOnly,
        String dateAddedMode,
        LocalDate dateAddedOn,
        Integer dateAddedYear,
        String lastAuditedMode,
        LocalDate lastAuditedOn,
        String sortBy,
        String sortDirection,
        int page,
        int size
) {
    /**
     * Converts the current page/size pair into a JDBC offset value.
     *
     * @return non-negative row offset
     */
    public int offset() {
        return Math.max(page, 0) * Math.max(size, 1);
    }

    /**
     * Returns the normalized sort field supported by the catalog list query.
     *
     * @return one of {@code title}, {@code author}, {@code year}, {@code dateAdded}, or {@code updated}
     */
    public String normalizedSortBy() {
        return switch (valueOrEmpty(sortBy)) {
            case "author" -> "author";
            case "year" -> "year";
            case "dateAdded" -> "dateAdded";
            case "updated" -> "updated";
            default -> "title";
        };
    }

    /**
     * Returns the normalized sort direction supported by the catalog list query.
     *
     * @return {@code asc} or {@code desc}
     */
    public String normalizedSortDirection() {
        return "desc".equalsIgnoreCase(valueOrEmpty(sortDirection)) ? "desc" : "asc";
    }

    /**
     * Returns the normalized last-audited comparison mode supported by the list query.
     *
     * @return {@code before}, {@code after}, {@code never}, or {@code any}
     */
    public String normalizedLastAuditedMode() {
        return switch (lowercaseValueOrEmpty(lastAuditedMode)) {
            case "before" -> "before";
            case "after" -> "after";
            case "never" -> "never";
            default -> "any";
        };
    }

    /**
     * Returns the normalized date-added comparison mode supported by the list query.
     *
     * @return {@code before}, {@code after}, {@code year}, or {@code any}
     */
    public String normalizedDateAddedMode() {
        return switch (lowercaseValueOrEmpty(dateAddedMode)) {
            case "before" -> "before";
            case "after" -> "after";
            case "year" -> "year";
            default -> "any";
        };
    }

    /**
     * Returns the normalized collection-combination mode used when more than
     * one collection filter is active.
     *
     * @return {@code and} or {@code or}
     */
    public String normalizedCollectionMode() {
        return "and".equalsIgnoreCase(valueOrEmpty(collectionMode)) ? "and" : "or";
    }

    private static String lowercaseValueOrEmpty(String value) {
        return valueOrEmpty(value).toLowerCase();
    }

    private static String valueOrEmpty(String value) {
        return Optional.ofNullable(value).orElse("");
    }
}
