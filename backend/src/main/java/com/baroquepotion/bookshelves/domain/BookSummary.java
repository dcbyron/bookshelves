package com.baroquepotion.bookshelves.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight book projection for list results.
 */
public record BookSummary(
        UUID id,
        String title,
        String author,
        String shelfCoords,
        Integer year,
        String shelfId,
        LocalDate dateAdded,
        OffsetDateTime updatedAt
) {
}
