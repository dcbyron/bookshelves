package com.baroquepotion.bookshelves.api.dto;

import java.math.BigDecimal;

/**
 * One row in a printable/exportable collection-value report.
 *
 * @param id stable collection identifier
 * @param name collection label shown in the UI
 * @param bookCount number of books currently assigned to the collection
 * @param totalValue summed value for the collection under the report's fallback rules
 */
public record CollectionValueReportRowResponse(
        String id,
        String name,
        long bookCount,
        BigDecimal totalValue
) {
}
