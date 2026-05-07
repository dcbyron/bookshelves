package com.baroquepotion.bookshelves.api.dto;

/**
 * Response payload describing a completed bulk publisher rename.
 */
public record PublisherRenameResponse(String publisher, long updatedCount) {
}
