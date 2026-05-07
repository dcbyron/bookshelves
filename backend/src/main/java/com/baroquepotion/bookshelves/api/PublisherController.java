package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.PublisherRenameRequest;
import com.baroquepotion.bookshelves.api.dto.PublisherRenameResponse;
import com.baroquepotion.bookshelves.api.dto.PublisherSummaryResponse;
import com.baroquepotion.bookshelves.application.PublisherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for normalizing publisher strings across book records.
 */
@RestController
@RequestMapping("/api/publishers")
public class PublisherController {

    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    /**
     * Returns all non-blank publisher strings with their current usage counts.
     *
     * @return publisher summaries ordered for display
     */
    @GetMapping
    public List<PublisherSummaryResponse> list() {
        return publisherService.list().stream()
                .map(publisher -> new PublisherSummaryResponse(publisher.publisher(), publisher.bookCount()))
                .toList();
    }

    /**
     * Rewrites one exact publisher string to a canonical replacement.
     *
     * @param request validated rename payload
     * @return replacement publisher and number of updated books
     */
    @PostMapping("/rename")
    public PublisherRenameResponse rename(@Valid @RequestBody PublisherRenameRequest request) {
        var result = publisherService.rename(request);
        return new PublisherRenameResponse(result.publisher(), result.updatedCount());
    }
}
