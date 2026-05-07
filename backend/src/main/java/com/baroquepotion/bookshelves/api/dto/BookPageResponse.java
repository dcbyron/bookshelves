package com.baroquepotion.bookshelves.api.dto;

import java.util.List;

/**
 * Paginated response for the catalog books list.
 *
 * @param items current page of book summaries
 * @param page zero-based page index
 * @param size configured page size
 * @param totalItems total number of matching books
 * @param totalPages total number of pages for the current filters
 */
public record BookPageResponse(
        List<BookSummaryResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
