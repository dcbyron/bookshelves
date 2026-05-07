package com.baroquepotion.bookshelves.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregate summary returned by a value analytics query.
 */
public record ValueSummary(
        BigDecimal totalValue,
        long includedCount,
        long missingValueCount,
        long totalScopedCount,
        long missingReplacementCheckedCount,
        LocalDate oldestReplacementChecked
) {
}
