package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.ValueDetailsResponse;
import com.baroquepotion.bookshelves.api.dto.ValueSummaryResponse;
import com.baroquepotion.bookshelves.application.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Focused endpoints for library value analytics.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Returns aggregate value totals and coverage information for the selected scope.
     *
     * @param basis selected value basis, defaulting to {@code paid}
     * @param collectionId optional collection scope
     * @param shelfId optional shelf scope
     * @param dateAddedMode optional date-added comparison mode
     * @param dateAddedOn optional date-added comparison date
     * @param dateAddedYear optional date-added year used with {@code dateAddedMode=year}
     * @param specialOnly whether to restrict the scope to special books
     * @param digitalOnly whether to restrict the scope to digital books
     * @param hardbackOnly whether to restrict the scope to hardback books
     * @return value summary payload
     */
    @GetMapping("/value-summary")
    public ValueSummaryResponse valueSummary(@RequestParam(defaultValue = "paid") String basis,
                                             @RequestParam(required = false) String collectionId,
                                             @RequestParam(required = false) String shelfId,
                                             @RequestParam(required = false) String dateAddedMode,
                                             @RequestParam(required = false) LocalDate dateAddedOn,
                                             @RequestParam(required = false) Integer dateAddedYear,
                                             @RequestParam(defaultValue = "false") boolean specialOnly,
                                             @RequestParam(defaultValue = "false") boolean digitalOnly,
                                             @RequestParam(defaultValue = "false") boolean hardbackOnly,
                                             @RequestParam(defaultValue = "false") boolean dustjacketOnly,
                                             @RequestParam(defaultValue = "false") boolean slipcaseOnly,
                                             @RequestParam(defaultValue = "false") boolean videoOnly) {
        return analyticsService.valueSummary(basis, collectionId, shelfId, dateAddedMode, dateAddedOn, dateAddedYear, specialOnly, digitalOnly, hardbackOnly, dustjacketOnly, slipcaseOnly, videoOnly);
    }

    /**
     * Returns the scoped books that contribute to the current analytics view.
     *
     * @param basis selected value basis, defaulting to {@code paid}
     * @param collectionId optional collection scope
     * @param shelfId optional shelf scope
     * @param dateAddedMode optional date-added comparison mode
     * @param dateAddedOn optional date-added comparison date
     * @param dateAddedYear optional date-added year used with {@code dateAddedMode=year}
     * @param specialOnly whether to restrict the scope to special books
     * @param digitalOnly whether to restrict the scope to digital books
     * @param hardbackOnly whether to restrict the scope to hardback books
     * @return detailed scoped rows including missing-value entries
     */
    @GetMapping("/value-details")
    public ValueDetailsResponse valueDetails(@RequestParam(defaultValue = "paid") String basis,
                                             @RequestParam(required = false) String collectionId,
                                             @RequestParam(required = false) String shelfId,
                                             @RequestParam(required = false) String dateAddedMode,
                                             @RequestParam(required = false) LocalDate dateAddedOn,
                                             @RequestParam(required = false) Integer dateAddedYear,
                                             @RequestParam(defaultValue = "false") boolean specialOnly,
                                             @RequestParam(defaultValue = "false") boolean digitalOnly,
                                             @RequestParam(defaultValue = "false") boolean hardbackOnly,
                                             @RequestParam(defaultValue = "false") boolean dustjacketOnly,
                                             @RequestParam(defaultValue = "false") boolean slipcaseOnly,
                                             @RequestParam(defaultValue = "false") boolean videoOnly) {
        return analyticsService.valueDetails(basis, collectionId, shelfId, dateAddedMode, dateAddedOn, dateAddedYear, specialOnly, digitalOnly, hardbackOnly, dustjacketOnly, slipcaseOnly, videoOnly);
    }
}
