package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.AuthorSummaryResponse;
import com.baroquepotion.bookshelves.api.dto.BookSummaryResponse;
import com.baroquepotion.bookshelves.api.dto.CollectionValueReportRowResponse;
import com.baroquepotion.bookshelves.api.dto.GroupedBooksResponse;
import com.baroquepotion.bookshelves.api.dto.MissingLocationReportResponse;
import com.baroquepotion.bookshelves.domain.BookSummary;
import com.baroquepotion.bookshelves.persistence.BookRepository;
import com.baroquepotion.bookshelves.persistence.CollectionRepository;
import com.baroquepotion.bookshelves.persistence.ShelfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coordinates read-only browse and reporting queries that sit above the core CRUD workflow.
 */
@Service
public class BrowseService {

    private final BookRepository bookRepository;
    private final CollectionRepository collectionRepository;
    private final ShelfRepository shelfRepository;

    public BrowseService(BookRepository bookRepository,
                         CollectionRepository collectionRepository,
                         ShelfRepository shelfRepository) {
        this.bookRepository = bookRepository;
        this.collectionRepository = collectionRepository;
        this.shelfRepository = shelfRepository;
    }

    /**
     * Builds author-browse buckets from the catalog.
     *
     * @return author summaries ordered for display
     */
    @Transactional(readOnly = true)
    public List<AuthorSummaryResponse> listAuthors() {
        return bookRepository.listAuthors().stream()
                .map(author -> new AuthorSummaryResponse(author.key(), author.name(), author.bookCount()))
                .toList();
    }

    /**
     * Loads books for one author bucket.
     *
     * @param authorKey stable author bucket key
     * @return matching books ordered for browse display
     */
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> booksForAuthor(String authorKey) {
        return bookRepository.findBooksByAuthorKey(authorKey).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Loads the most recently added books.
     *
     * @param limit maximum number of books to return
     * @return recent additions ordered by date added
     */
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> recentlyAdded(int limit) {
        return bookRepository.findRecentlyAdded(limit).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Loads the most recently updated books.
     *
     * @param limit maximum number of books to return
     * @return recent updates ordered by update timestamp
     */
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> recentlyUpdated(int limit) {
        return bookRepository.findRecentlyUpdated(limit).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private List<GroupedBooksResponse> groupedShelfBooks() {
        Map<String, List<BookSummary>> booksByShelfId = bookRepository.findBooksByShelf().stream()
                .collect(Collectors.groupingBy(BookSummary::shelfId));
        var groups = shelfRepository.findAll().stream()
                .map(shelf -> new GroupedBooksResponse(
                        shelf.id(),
                        shelf.name(),
                        shelf.usageCount(),
                        booksByShelfId.getOrDefault(shelf.id(), List.of()).stream()
                                .map(this::toSummaryResponse)
                                .toList()))
                .collect(Collectors.toList());

        var unassignedBooks = bookRepository.findMissingLocationBooks();
        if (!unassignedBooks.isEmpty()) {
            groups.add(new GroupedBooksResponse(
                    null,
                    "Unassigned",
                    unassignedBooks.size(),
                    unassignedBooks.stream().map(this::toSummaryResponse).toList()
            ));
        }
        return groups;
    }

    /**
     * Returns the printable shelf report.
     *
     * @return shelf-grouped report rows
     */
    @Transactional(readOnly = true)
    public List<GroupedBooksResponse> shelfReport() {
        return groupedShelfBooks();
    }

    /**
     * Returns the printable collection report.
     *
     * @return collection-grouped report rows
     */
    @Transactional(readOnly = true)
    public List<GroupedBooksResponse> collectionReport() {
        Map<String, List<BookSummary>> booksByCollectionId = bookRepository.findBooksByCollection().stream()
                .collect(Collectors.groupingBy(BookSummary::shelfId));
        return collectionRepository.findAll().stream()
                .map(collection -> new GroupedBooksResponse(
                        collection.id(),
                        collection.name(),
                        collection.usageCount(),
                        booksByCollectionId.getOrDefault(collection.id(), List.of()).stream()
                                .map(this::toSummaryResponse)
                                .toList()))
                .toList();
    }

    /**
     * Returns the printable report for books without a shelf assignment.
     *
     * @return missing-location report payload
     */
    @Transactional(readOnly = true)
    public MissingLocationReportResponse missingLocationReport() {
        var books = bookRepository.findMissingLocationBooks().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new MissingLocationReportResponse(books.size(), books);
    }

    /**
     * Returns a printable collection-value report using paid-first fallback.
     *
     * @return collection rows with sunk-cost totals
     */
    @Transactional(readOnly = true)
    public List<CollectionValueReportRowResponse> valueByCollectionPaidReport() {
        return bookRepository.findValueByCollectionPaidReport().stream()
                .map(row -> new CollectionValueReportRowResponse(row.id(), row.name(), row.bookCount(), row.totalValue()))
                .toList();
    }

    /**
     * Returns a printable collection-value report using replacement-first fallback.
     *
     * @return collection rows with insurance-value totals
     */
    @Transactional(readOnly = true)
    public List<CollectionValueReportRowResponse> valueByCollectionReplacementReport() {
        return bookRepository.findValueByCollectionReplacementReport().stream()
                .map(row -> new CollectionValueReportRowResponse(row.id(), row.name(), row.bookCount(), row.totalValue()))
                .toList();
    }

    private BookSummaryResponse toSummaryResponse(BookSummary book) {
        return new BookSummaryResponse(
                book.id(),
                book.title(),
                book.author(),
                book.shelfCoords(),
                book.year(),
                book.shelfId(),
                book.dateAdded(),
                book.updatedAt()
        );
    }
}
