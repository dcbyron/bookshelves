package com.baroquepotion.bookshelves.domain;

/**
 * Shelf vocabulary entry describing a physical placement location.
 */
public record Shelf(String id, String name, boolean audited, long usageCount) {
}
