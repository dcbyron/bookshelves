package com.baroquepotion.bookshelves.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detailed book row included in a value analytics result set.
 */
public record ValueDetail(
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
