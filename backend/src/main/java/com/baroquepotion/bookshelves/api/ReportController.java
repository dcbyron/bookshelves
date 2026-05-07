package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.GroupedBooksResponse;
import com.baroquepotion.bookshelves.api.dto.MissingLocationReportResponse;
import com.baroquepotion.bookshelves.api.dto.CollectionValueReportRowResponse;
import com.baroquepotion.bookshelves.application.BrowseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Printable and exportable report endpoints.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final BrowseService browseService;

    public ReportController(BrowseService browseService) {
        this.browseService = browseService;
    }

    /**
     * Returns a printable or exportable shelf-grouped report.
     *
     * @return shelf report rows including grouped book summaries
     */
    @GetMapping("/shelves")
    public List<GroupedBooksResponse> shelves() {
        return browseService.shelfReport();
    }

    /**
     * Returns a printable or exportable collection-grouped report.
     *
     * @return collection report rows including grouped book summaries
     */
    @GetMapping("/collections")
    public List<GroupedBooksResponse> collections() {
        return browseService.collectionReport();
    }

    /**
     * Returns a report of books that do not currently have a shelf assignment.
     *
     * @return missing-location report payload
     */
    @GetMapping("/missing-location")
    public MissingLocationReportResponse missingLocation() {
        return browseService.missingLocationReport();
    }

    /**
     * Returns printable sunk-cost totals by collection.
     *
     * @return collection rows valued with paid, printed, replacement fallback
     */
    @GetMapping("/value-by-collection-paid")
    public List<CollectionValueReportRowResponse> valueByCollectionPaid() {
        return browseService.valueByCollectionPaidReport();
    }

    /**
     * Returns printable insurance-value totals by collection.
     *
     * @return collection rows valued with replacement, paid, printed fallback
     */
    @GetMapping("/value-by-collection-replacement")
    public List<CollectionValueReportRowResponse> valueByCollectionReplacement() {
        return browseService.valueByCollectionReplacementReport();
    }
}
