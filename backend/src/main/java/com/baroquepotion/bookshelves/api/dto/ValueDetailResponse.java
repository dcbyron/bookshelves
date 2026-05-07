package com.baroquepotion.bookshelves.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detailed row returned by the value analytics drill-down endpoint.
 *
 * @param id book identifier
 * @param title book title
 * @param author primary author text
 * @param year publication year
 * @param shelfId physical shelf identifier when present
 * @param dateAdded date the book was added to the catalog
 * @param updatedAt last update timestamp
 * @param valueAmount selected monetary value for the active basis, or {@code null} when missing
 * @param priceToReplaceChecked date the replacement estimate was last reviewed
 * @param special whether the book is marked special
 * @param digital whether the book is marked digital
 * @param hardback whether the book is marked hardback
 * @param dustjacket whether the book is marked dustjacket
 * @param slipcase whether the book is marked slipcase
 * @param video whether the book is marked video
 */
public record ValueDetailResponse(
        UUID id,
        String title,
        String author,
        Integer year,
        String shelfId,
        LocalDate dateAdded,
        OffsetDateTime updatedAt,
        BigDecimal valueAmount,
        LocalDate priceToReplaceChecked,
        boolean special,
        boolean digital,
        Boolean hardback,
        boolean dustjacket,
        boolean slipcase,
        boolean video
) {
}
