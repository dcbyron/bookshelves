package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.ValueDetailResponse;
import com.baroquepotion.bookshelves.api.dto.ValueDetailsResponse;
import com.baroquepotion.bookshelves.api.dto.ValueSummaryResponse;
import com.baroquepotion.bookshelves.domain.ValueAnalyticsCriteria;
import com.baroquepotion.bookshelves.persistence.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Coordinates focused library value analytics queries and coverage calculations.
 */
@Service
public class AnalyticsService {

    private final BookRepository bookRepository;

    public AnalyticsService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Builds an aggregate summary for one analytics scope.
     *
     * @param basis selected value basis
     * @param collectionId optional collection scope
     * @param shelfId optional shelf scope
     * @param dateAddedMode optional date-added comparison mode
     * @param dateAddedOn optional date-added comparison date
     * @param dateAddedYear optional date-added year used with {@code dateAddedMode=year}
     * @param specialOnly whether to restrict to special books
     * @param digitalOnly whether to restrict to digital books
     * @param hardbackOnly whether to restrict to hardback books
     * @return aggregate value summary
     */
    @Transactional(readOnly = true)
    public ValueSummaryResponse valueSummary(String basis,
                                             String collectionId,
                                             String shelfId,
                                             String dateAddedMode,
                                             LocalDate dateAddedOn,
                                             Integer dateAddedYear,
                                             boolean specialOnly,
                                             boolean digitalOnly,
                                             boolean hardbackOnly,
                                             boolean dustjacketOnly,
                                             boolean slipcaseOnly,
                                             boolean videoOnly) {
        var criteria = new ValueAnalyticsCriteria(basis, collectionId, shelfId, dateAddedMode, dateAddedOn, dateAddedYear, specialOnly, digitalOnly, hardbackOnly, dustjacketOnly, slipcaseOnly, videoOnly);
        var summary = bookRepository.fetchValueSummary(criteria);
        return new ValueSummaryResponse(
                criteria.normalizedBasis(),
                summary.totalValue(),
                summary.includedCount(),
                summary.missingValueCount(),
                summary.totalScopedCount(),
                coveragePercent(summary.includedCount(), summary.totalScopedCount()),
                summary.missingReplacementCheckedCount(),
                summary.oldestReplacementChecked()
        );
    }

    /**
     * Builds the book-level drill-down rows for one analytics scope.
     *
     * @param basis selected value basis
     * @param collectionId optional collection scope
     * @param shelfId optional shelf scope
     * @param dateAddedMode optional date-added comparison mode
     * @param dateAddedOn optional date-added comparison date
     * @param dateAddedYear optional date-added year used with {@code dateAddedMode=year}
     * @param specialOnly whether to restrict to special books
     * @param digitalOnly whether to restrict to digital books
     * @param hardbackOnly whether to restrict to hardback books
     * @return scoped book rows
     */
    @Transactional(readOnly = true)
    public ValueDetailsResponse valueDetails(String basis,
                                             String collectionId,
                                             String shelfId,
                                             String dateAddedMode,
                                             LocalDate dateAddedOn,
                                             Integer dateAddedYear,
                                             boolean specialOnly,
                                             boolean digitalOnly,
                                             boolean hardbackOnly,
                                             boolean dustjacketOnly,
                                             boolean slipcaseOnly,
                                             boolean videoOnly) {
        var criteria = new ValueAnalyticsCriteria(basis, collectionId, shelfId, dateAddedMode, dateAddedOn, dateAddedYear, specialOnly, digitalOnly, hardbackOnly, dustjacketOnly, slipcaseOnly, videoOnly);
        var items = bookRepository.fetchValueDetails(criteria).stream()
                .map(detail -> new ValueDetailResponse(
                        detail.id(),
                        detail.title(),
                        detail.author(),
                        detail.year(),
                        detail.shelfId(),
                        detail.dateAdded(),
                        detail.updatedAt(),
                        detail.valueAmount(),
                        detail.priceToReplaceChecked(),
                        detail.special(),
                        detail.digital(),
                        detail.hardback(),
                        detail.dustjacket(),
                        detail.slipcase(),
                        detail.video()
                ))
                .toList();
        return new ValueDetailsResponse(criteria.normalizedBasis(), items.size(), items);
    }

    private BigDecimal coveragePercent(long includedCount, long totalScopedCount) {
        if (totalScopedCount <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(includedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalScopedCount), 2, RoundingMode.HALF_UP);
    }
}
