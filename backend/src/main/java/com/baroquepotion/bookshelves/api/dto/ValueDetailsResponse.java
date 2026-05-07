package com.baroquepotion.bookshelves.api.dto;

import java.util.List;

/**
 * Drill-down payload for value analytics.
 *
 * @param basis selected value basis used for the rows
 * @param totalItems number of scoped books represented by the result
 * @param items scoped book rows, including books missing the selected monetary value
 */
public record ValueDetailsResponse(
        String basis,
        long totalItems,
        List<ValueDetailResponse> items
) {
}
