package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.BookPageResponse;
import com.baroquepotion.bookshelves.api.dto.BookNavigationResponse;
import com.baroquepotion.bookshelves.api.dto.BookRequest;
import com.baroquepotion.bookshelves.api.dto.BookResponse;
import com.baroquepotion.bookshelves.api.dto.BookSummaryResponse;
import com.baroquepotion.bookshelves.application.BookService;
import com.baroquepotion.bookshelves.domain.BookSearchCriteria;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for browsing and maintaining books in the catalog.
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Returns a filtered, paginated list of catalog books for the main catalog view.
     *
     * @param q free-text title/author search
     * @param collectionIds optional collection filters
     * @param collectionMode how multiple selected collections combine
     * @param noCollectionOnly optional filter for books with no collection assignments
     * @param noBestEstimateOnly optional filter for books with no non-zero value estimate
     * @param shelfId optional shelf filter
     * @param year optional publication year filter
     * @param dateAddedMode optional date-added comparison mode
     * @param dateAddedOn optional date-added comparison date
     * @param dateAddedYear optional date-added year used with {@code dateAddedMode=year}
     * @param lastAuditedMode optional last-audited comparison mode
     * @param lastAuditedOn optional last-audited comparison date
     * @param page zero-based page index
     * @param size number of rows to return
     * @return book summaries matching the requested criteria
     */
    @GetMapping
    public BookPageResponse list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) List<String> collectionIds,
                                 @RequestParam(defaultValue = "or") String collectionMode,
                                 @RequestParam(required = false) Boolean noCollectionOnly,
                                 @RequestParam(required = false) Boolean noBestEstimateOnly,
                                 @RequestParam(required = false) String shelfId,
                                 @RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) Boolean hasShelf,
                                 @RequestParam(required = false) Boolean specialOnly,
                                 @RequestParam(required = false) Boolean digitalOnly,
                                 @RequestParam(required = false) Boolean hardbackOnly,
                                 @RequestParam(required = false) Boolean dustjacketOnly,
                                 @RequestParam(required = false) Boolean slipcaseOnly,
                                 @RequestParam(required = false) Boolean videoOnly,
                                 @RequestParam(required = false) String dateAddedMode,
                                 @RequestParam(required = false) LocalDate dateAddedOn,
                                 @RequestParam(required = false) Integer dateAddedYear,
                                 @RequestParam(required = false) String lastAuditedMode,
                                 @RequestParam(required = false) LocalDate lastAuditedOn,
                                 @RequestParam(defaultValue = "title") String sort,
                                 @RequestParam(defaultValue = "asc") String direction,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "25") int size) {
        var criteria = new BookSearchCriteria(
                q,
                collectionIds,
                collectionMode,
                noCollectionOnly,
                noBestEstimateOnly,
                shelfId,
                year,
                hasShelf,
                specialOnly,
                digitalOnly,
                hardbackOnly,
                dustjacketOnly,
                slipcaseOnly,
                videoOnly,
                dateAddedMode,
                dateAddedOn,
                dateAddedYear,
                lastAuditedMode,
                lastAuditedOn,
                sort,
                direction,
                page,
                size
        );
        var result = bookService.search(criteria);
        var items = result.books().stream()
                .map(book -> new BookSummaryResponse(
                        book.id(),
                        book.title(),
                        book.author(),
                        book.shelfCoords(),
                        book.year(),
                        book.shelfId(),
                        book.dateAdded(),
                        book.updatedAt()))
                .toList();
        int totalPages = (int) Math.ceil((double) result.totalCount() / Math.max(size, 1));
        return new BookPageResponse(items, page, size, result.totalCount(), totalPages);
    }

    /**
     * Returns previous/next navigation metadata for one current book inside the
     * filtered and sorted catalog working set.
     *
     * @param id current book id inside the preserved Books working set
     * @param q free-text title/author search
     * @param collectionIds optional collection filters
     * @param collectionMode how multiple selected collections combine
     * @param noCollectionOnly optional filter for books with no collection assignments
     * @param noBestEstimateOnly optional filter for books with no non-zero value estimate
     * @param shelfId optional shelf filter
     * @param year optional publication year filter
     * @param dateAddedMode optional date-added comparison mode
     * @param dateAddedOn optional date-added comparison date
     * @param dateAddedYear optional date-added year used with {@code dateAddedMode=year}
     * @param lastAuditedMode optional last-audited comparison mode
     * @param lastAuditedOn optional last-audited comparison date
     * @return previous/next ids plus current position inside the matching result set
     */
    @GetMapping("/{id}/navigation")
    public BookNavigationResponse navigation(@PathVariable UUID id,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) List<String> collectionIds,
                                             @RequestParam(defaultValue = "or") String collectionMode,
                                             @RequestParam(required = false) Boolean noCollectionOnly,
                                             @RequestParam(required = false) Boolean noBestEstimateOnly,
                                             @RequestParam(required = false) String shelfId,
                                             @RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Boolean hasShelf,
                                             @RequestParam(required = false) Boolean specialOnly,
                                             @RequestParam(required = false) Boolean digitalOnly,
                                             @RequestParam(required = false) Boolean hardbackOnly,
                                             @RequestParam(required = false) Boolean dustjacketOnly,
                                             @RequestParam(required = false) Boolean slipcaseOnly,
                                             @RequestParam(required = false) Boolean videoOnly,
                                             @RequestParam(required = false) String dateAddedMode,
                                             @RequestParam(required = false) LocalDate dateAddedOn,
                                             @RequestParam(required = false) Integer dateAddedYear,
                                             @RequestParam(required = false) String lastAuditedMode,
                                             @RequestParam(required = false) LocalDate lastAuditedOn,
                                             @RequestParam(defaultValue = "title") String sort,
                                             @RequestParam(defaultValue = "asc") String direction) {
        var navigation = bookService.navigation(id, new BookSearchCriteria(
                q,
                collectionIds,
                collectionMode,
                noCollectionOnly,
                noBestEstimateOnly,
                shelfId,
                year,
                hasShelf,
                specialOnly,
                digitalOnly,
                hardbackOnly,
                dustjacketOnly,
                slipcaseOnly,
                videoOnly,
                dateAddedMode,
                dateAddedOn,
                dateAddedYear,
                lastAuditedMode,
                lastAuditedOn,
                sort,
                direction,
                0,
                25
        ));
        return new BookNavigationResponse(
                navigation.previousBookId(),
                navigation.nextBookId(),
                navigation.position(),
                navigation.totalItems()
        );
    }

    /**
     * Loads a single book including collection assignments and audit metadata.
     *
     * @param id unique book identifier
     * @return the requested book
     */
    @GetMapping("/{id}")
    public BookResponse get(@PathVariable UUID id) {
        var book = bookService.getById(id);
        return new BookResponse(
                book.id(),
                book.title(),
                book.author(),
                book.publicationCity(),
                book.publisher(),
                book.imprint(),
                book.year(),
                book.firstPublished(),
                book.edition(),
                book.pages(),
                book.frontpages(),
                book.dateAdded(),
                book.lastAudited(),
                book.shelfId(),
                book.authorSecondary(),
                book.isbn(),
                book.special(),
                book.digital(),
                book.hardback(),
                book.dustjacket(),
                book.slipcase(),
                book.video(),
                book.vols(),
                book.series(),
                book.shelfCoords(),
                book.note(),
                book.pricePaid(),
                book.priceOnItem(),
                book.priceToReplace(),
                book.priceToReplaceChecked(),
                book.createdAt(),
                book.updatedAt(),
                book.collectionIds()
        );
    }

    /**
     * Creates a new book record.
     *
     * @param request validated book payload supplied by the client
     * @return the created book as stored in the database
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        var book = bookService.create(request);
        return get(book.id());
    }

    /**
     * Replaces the editable fields of an existing book.
     *
     * @param id unique book identifier
     * @param request validated replacement payload
     * @return the updated book as stored in the database
     */
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        var book = bookService.update(id, request);
        return get(book.id());
    }

    /**
     * Deletes a book and its collection assignments.
     *
     * @param id unique book identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        bookService.delete(id);
    }
}
