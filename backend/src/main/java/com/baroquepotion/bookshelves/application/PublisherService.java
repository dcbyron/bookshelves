package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.PublisherRenameRequest;
import com.baroquepotion.bookshelves.domain.PublisherRenameResult;
import com.baroquepotion.bookshelves.domain.PublisherSummary;
import com.baroquepotion.bookshelves.persistence.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coordinates publisher-maintenance workflows over book records.
 */
@Service
public class PublisherService {

    private final BookRepository bookRepository;

    public PublisherService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Returns all non-blank publisher buckets with usage counts.
     *
     * @return publisher summaries ordered for display
     */
    @Transactional(readOnly = true)
    public List<PublisherSummary> list() {
        return bookRepository.listPublishers();
    }

    /**
     * Rewrites one exact publisher string to another across all matching books.
     *
     * @param request validated rename payload
     * @return rename result with the number of updated books
     */
    @Transactional
    public PublisherRenameResult rename(PublisherRenameRequest request) {
        String sourcePublisher = normalizeRequired(request.sourcePublisher(), "Source publisher");
        String targetPublisher = normalizeRequired(request.targetPublisher(), "Target publisher");
        if (sourcePublisher.equals(targetPublisher)) {
            return new PublisherRenameResult(targetPublisher, 0);
        }
        return new PublisherRenameResult(targetPublisher, bookRepository.renamePublisher(sourcePublisher, targetPublisher));
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
