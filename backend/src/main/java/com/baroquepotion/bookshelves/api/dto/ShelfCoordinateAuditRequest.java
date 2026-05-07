package com.baroquepotion.bookshelves.api.dto;

/**
 * Request payload for updating the audit state of one shelf coordinate.
 */
public record ShelfCoordinateAuditRequest(Boolean audited) {
}
