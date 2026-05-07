package com.baroquepotion.bookshelves.persistence;

/**
 * Summary row for author browsing.
 *
 * @param key stable identifier used by the API
 * @param name display label shown in the UI
 * @param bookCount number of books in the bucket
 */
public record AuthorSummary(String key, String name, long bookCount) {
}
