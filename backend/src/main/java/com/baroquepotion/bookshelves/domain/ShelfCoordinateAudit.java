package com.baroquepotion.bookshelves.domain;

/**
 * Audit state for one occupied coordinate tier on a physical shelf.
 */
public record ShelfCoordinateAudit(String shelfId, String shelfName, String shelfCoords, boolean audited) {
}
