package com.baroquepotion.bookshelves.api.dto;

import java.util.List;

/**
 * Report payload for books that do not currently have a shelf assignment.
 *
 * @param totalItems number of matching books
 * @param books books without a physical location
 */
public record MissingLocationReportResponse(long totalItems, List<BookSummaryResponse> books) {
}
