package com.baroquepotion.bookshelves.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full book representation returned by the REST API.
 */
public record BookResponse(
        UUID id,
        String title,
        String author,
        String publicationCity,
        String publisher,
        String imprint,
        Integer year,
        Integer firstPublished,
        String edition,
        Integer pages,
        String frontpages,
        LocalDate dateAdded,
        LocalDate lastAudited,
        String shelfId,
        String authorSecondary,
        String isbn,
        boolean special,
        boolean digital,
        Boolean hardback,
        boolean dustjacket,
        boolean slipcase,
        boolean video,
        Integer vols,
        String series,
        String shelfCoords,
        String note,
        BigDecimal pricePaid,
        BigDecimal priceOnItem,
        BigDecimal priceToReplace,
        LocalDate priceToReplaceChecked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> collectionIds
) {
}
