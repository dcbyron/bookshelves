package com.baroquepotion.bookshelves.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Compact book projection returned by list endpoints.
 */
public record BookSummaryResponse(
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
