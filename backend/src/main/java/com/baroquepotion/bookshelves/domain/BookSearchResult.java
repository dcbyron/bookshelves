package com.baroquepotion.bookshelves.domain;

import java.util.List;

/**
 * Paginated result of a book search.
 *
 * @param books current page of matching book summaries
 * @param totalCount total number of matching rows across all pages
 */
public record BookSearchResult(List<BookSummary> books, long totalCount) {
}
