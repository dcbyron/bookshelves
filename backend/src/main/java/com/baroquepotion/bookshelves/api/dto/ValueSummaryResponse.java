package com.baroquepotion.bookshelves.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregate summary payload for a library value analytics query.
 *
 * @param basis selected value basis
 * @param totalValue summed monetary value across scoped books with a value present
 * @param includedCount number of scoped books included in the sum
 * @param missingValueCount number of scoped books missing the selected value
 * @param totalScopedCount total number of books in scope before missing-value exclusion
 * @param coveragePercent percentage of scoped books that contributed to the sum
 * @param missingReplacementCheckedCount number of scoped books with a replacement price but no checked date
 * @param oldestReplacementChecked oldest replacement review date among scoped books with both fields present
 */
public record ValueSummaryResponse(
        String basis,
        BigDecimal totalValue,
        long includedCount,
        long missingValueCount,
        long totalScopedCount,
        BigDecimal coveragePercent,
        long missingReplacementCheckedCount,
        LocalDate oldestReplacementChecked
) {
}
