package com.baroquepotion.bookshelves.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for creating or updating a book.
 */
public record BookRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 500) String author,
        @Size(max = 255) String publicationCity,
        @Size(max = 1000) String publisher,
        @Size(max = 1000) String imprint,
        @Min(0) Integer year,
        @Min(0) Integer firstPublished,
        @Size(max = 255) String edition,
        @Min(0) Integer pages,
        @Size(max = 255) String frontpages,
        LocalDate dateAdded,
        LocalDate lastAudited,
        String shelfId,
        @Size(max = 500) String authorSecondary,
        @Size(max = 64) String isbn,
        Boolean special,
        Boolean digital,
        Boolean hardback,
        Boolean dustjacket,
        Boolean slipcase,
        Boolean video,
        @Min(0) Integer vols,
        @Size(max = 255) String series,
        @Size(max = 255) String shelfCoords,
        @Size(max = 4000) String note,
        @DecimalMin("0.0") BigDecimal pricePaid,
        @DecimalMin("0.0") BigDecimal priceOnItem,
        @DecimalMin("0.0") BigDecimal priceToReplace,
        LocalDate priceToReplaceChecked,
        List<String> collectionIds
) {
}
