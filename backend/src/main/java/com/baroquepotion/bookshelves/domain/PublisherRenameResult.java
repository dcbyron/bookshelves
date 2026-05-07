package com.baroquepotion.bookshelves.domain;

/**
 * Result of bulk-renaming one publisher string across existing books.
 *
 * @param publisher replacement publisher string
 * @param updatedCount number of books updated
 */
public record PublisherRenameResult(String publisher, long updatedCount) {
}
