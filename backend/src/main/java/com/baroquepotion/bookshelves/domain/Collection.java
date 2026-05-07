package com.baroquepotion.bookshelves.domain;

/**
 * Named collection vocabulary entry assignable to books.
 */
public record Collection(String id, String name, long usageCount) {
}
