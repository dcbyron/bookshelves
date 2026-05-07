package com.baroquepotion.bookshelves.domain;

import java.math.BigDecimal;

/**
 * Printable/exportable collection-level value row.
 */
public record CollectionValueReportRow(
        String id,
        String name,
        long bookCount,
        BigDecimal totalValue
) {
}
