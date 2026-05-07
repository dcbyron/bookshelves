package com.baroquepotion.bookshelves.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a shelf definition.
 */
public record ShelfRequest(
        @NotBlank @Size(max = 100) String id,
        @NotBlank @Size(max = 255) String name,
        Boolean audited
) {
}
