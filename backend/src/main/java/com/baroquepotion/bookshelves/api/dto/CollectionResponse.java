package com.baroquepotion.bookshelves.api.dto;

/**
 * Collection representation returned by the REST API.
 */
public record CollectionResponse(String id, String name, long usageCount) {
}
