package com.baroquepotion.bookshelves.api.dto;

/**
 * Shelf representation returned by the REST API.
 */
public record ShelfResponse(String id, String name, boolean audited, long usageCount) {
}
