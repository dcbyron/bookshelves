package com.baroquepotion.bookshelves.api.dto;

/**
 * Summary row for the author browse screen.
 *
 * @param key stable identifier used when requesting books for an author bucket
 * @param name human-readable author label
 * @param bookCount number of books in the bucket
 */
public record AuthorSummaryResponse(String key, String name, long bookCount) {
}
