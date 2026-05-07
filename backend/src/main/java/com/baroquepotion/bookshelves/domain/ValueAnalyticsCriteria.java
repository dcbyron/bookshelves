package com.baroquepotion.bookshelves.domain;

import java.time.LocalDate;

/**
 * Shared filter and basis values for library value analytics queries.
 */
public record ValueAnalyticsCriteria(
        String basis,
        String collectionId,
        String shelfId,
        String dateAddedMode,
        LocalDate dateAddedOn,
        Integer dateAddedYear,
        boolean specialOnly,
        boolean digitalOnly,
        boolean hardbackOnly,
        boolean dustjacketOnly,
        boolean slipcaseOnly,
        boolean videoOnly
) {
    /**
     * Returns the normalized value basis supported by analytics endpoints.
     *
     * @return {@code paid}, {@code printed}, {@code replacement}, or
     * {@code bestEstimate}
     */
    public String normalizedBasis() {
        return switch (basis == null ? "" : basis) {
            case "printed" -> "printed";
            case "replacement" -> "replacement";
            case "bestEstimate" -> "bestEstimate";
            default -> "paid";
        };
    }

    /**
     * Returns the normalized date-added comparison mode supported by analytics
     * endpoints.
     *
     * @return {@code before}, {@code after}, {@code year}, or {@code any}
     */
    public String normalizedDateAddedMode() {
        return switch (dateAddedMode == null ? "" : dateAddedMode.toLowerCase()) {
            case "before" -> "before";
            case "after" -> "after";
            case "year" -> "year";
            default -> "any";
        };
    }
}
