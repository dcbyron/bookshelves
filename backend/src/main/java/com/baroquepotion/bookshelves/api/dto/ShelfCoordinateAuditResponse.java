package com.baroquepotion.bookshelves.api.dto;

/**
 * Response payload for a shelf coordinate audit row.
 */
public record ShelfCoordinateAuditResponse(String shelfId, String shelfName, String shelfCoords, boolean audited) {
}
