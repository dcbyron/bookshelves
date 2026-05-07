package com.baroquepotion.bookshelves.api.dto;

/**
 * Publisher summary returned by the publisher-maintenance endpoints.
 */
public record PublisherSummaryResponse(String publisher, long bookCount) {
}
