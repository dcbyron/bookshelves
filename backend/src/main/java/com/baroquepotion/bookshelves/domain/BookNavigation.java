package com.baroquepotion.bookshelves.domain;

import java.util.UUID;

/**
 * Relative position of one book inside a filtered and sorted catalog result set.
 *
 * @param previousBookId preceding book identifier, when one exists
 * @param nextBookId following book identifier, when one exists
 * @param position one-based position of the current book within the matching set
 * @param totalItems total number of matching books in the working set
 */
public record BookNavigation(
        UUID previousBookId,
        UUID nextBookId,
        Integer position,
        Long totalItems
) {
}
