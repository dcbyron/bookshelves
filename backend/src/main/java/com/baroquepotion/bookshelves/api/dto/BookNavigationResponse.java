package com.baroquepotion.bookshelves.api.dto;

import java.util.UUID;

/**
 * Neighboring-book navigation metadata for one filtered and sorted catalog working set.
 *
 * @param previousBookId preceding book identifier, when one exists
 * @param nextBookId following book identifier, when one exists
 * @param position one-based position of the current book within the working set
 * @param totalItems total number of matching books in the working set
 */
public record BookNavigationResponse(
        UUID previousBookId,
        UUID nextBookId,
        Integer position,
        Long totalItems
) {
}
