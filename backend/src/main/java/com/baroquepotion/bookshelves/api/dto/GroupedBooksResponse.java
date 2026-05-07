package com.baroquepotion.bookshelves.api.dto;

import java.util.List;

/**
 * Group of books used by grouped report endpoints.
 *
 * @param id stable group identifier when present
 * @param name group label shown in the UI
 * @param bookCount number of books in the group
 * @param books ordered books that belong to the group
 */
public record GroupedBooksResponse(String id, String name, long bookCount, List<BookSummaryResponse> books) {
}
