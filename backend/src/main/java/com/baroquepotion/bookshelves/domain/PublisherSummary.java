package com.baroquepotion.bookshelves.domain;

/**
 * Aggregated publisher usage information for maintenance views.
 *
 * @param publisher canonical publisher string currently stored on matching books
 * @param bookCount number of books using that exact publisher value
 */
public record PublisherSummary(String publisher, long bookCount) {
}
