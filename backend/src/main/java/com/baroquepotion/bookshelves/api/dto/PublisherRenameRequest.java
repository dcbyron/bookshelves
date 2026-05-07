package com.baroquepotion.bookshelves.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for bulk-renaming one publisher string across matching books.
 */
public record PublisherRenameRequest(
        @NotBlank @Size(max = 255) String sourcePublisher,
        @NotBlank @Size(max = 255) String targetPublisher
) {
}
