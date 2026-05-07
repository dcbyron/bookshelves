package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.AuthorSummaryResponse;
import com.baroquepotion.bookshelves.api.dto.BookSummaryResponse;
import com.baroquepotion.bookshelves.application.BrowseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only browsing endpoints beyond the main catalog list.
 */
@RestController
@RequestMapping("/api/browse")
public class BrowseController {

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    /**
     * Lists author buckets for the browse screen, including an unspecified-author bucket when needed.
     *
     * @return browseable author summaries ordered by display label
     */
    @GetMapping("/authors")
    public List<AuthorSummaryResponse> authors() {
        return browseService.listAuthors();
    }

    /**
     * Lists books that belong to one author bucket.
     *
     * @param authorKey stable key returned by {@link #authors()}
     * @return books for the selected author ordered for display
     */
    @GetMapping("/authors/{authorKey}/books")
    public List<BookSummaryResponse> authorBooks(@PathVariable String authorKey) {
        return browseService.booksForAuthor(authorKey);
    }

    /**
     * Returns the most recently added books.
     *
     * @param limit maximum number of books to return
     * @return books ordered by date-added recency
     */
    @GetMapping("/recently-added")
    public List<BookSummaryResponse> recentlyAdded(@RequestParam(defaultValue = "50") int limit) {
        return browseService.recentlyAdded(limit);
    }

    /**
     * Returns the most recently updated books.
     *
     * @param limit maximum number of books to return
     * @return books ordered by update recency
     */
    @GetMapping("/recently-updated")
    public List<BookSummaryResponse> recentlyUpdated(@RequestParam(defaultValue = "50") int limit) {
        return browseService.recentlyUpdated(limit);
    }

}
