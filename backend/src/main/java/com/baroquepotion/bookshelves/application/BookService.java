package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.BookRequest;
import com.baroquepotion.bookshelves.domain.Book;
import com.baroquepotion.bookshelves.domain.BookNavigation;
import com.baroquepotion.bookshelves.domain.BookSearchCriteria;
import com.baroquepotion.bookshelves.domain.BookSearchResult;
import com.baroquepotion.bookshelves.persistence.BookRepository;
import com.baroquepotion.bookshelves.persistence.CollectionRepository;
import com.baroquepotion.bookshelves.persistence.ShelfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates book-focused catalog operations and reference validation.
 */
@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final CollectionRepository collectionRepository;
    private final ShelfRepository shelfRepository;

    public BookService(BookRepository bookRepository,
                       CollectionRepository collectionRepository,
                       ShelfRepository shelfRepository) {
        this.bookRepository = bookRepository;
        this.collectionRepository = collectionRepository;
        this.shelfRepository = shelfRepository;
    }

    /**
     * Searches books using the criteria required by the catalog list screen.
     *
     * @param criteria filters and paging values supplied by the caller
     * @return summaries of matching books
     */
    @Transactional(readOnly = true)
    public BookSearchResult search(BookSearchCriteria criteria) {
        return bookRepository.search(normalizeCriteria(criteria));
    }

    /**
     * Loads a book by identifier.
     *
     * @param id unique book identifier
     * @return the stored book
     * @throws ResourceNotFoundException when the identifier is unknown
     */
    @Transactional(readOnly = true)
    public Book getById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
    }

    /**
     * Returns neighboring-book navigation metadata inside one filtered and
     * sorted catalog working set.
     *
     * @param id current book identifier
     * @param criteria active filter and sort context
     * @return neighboring-book navigation metadata, or an empty out-of-scope payload
     */
    @Transactional(readOnly = true)
    public BookNavigation navigation(UUID id, BookSearchCriteria criteria) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found: " + id);
        }
        return bookRepository.findNavigation(id, normalizeCriteria(criteria))
                .orElse(new BookNavigation(null, null, null, 0L));
    }

    /**
     * Creates a new book after validating referenced shelves and collections.
     *
     * @param request client-supplied book payload
     * @return persisted book including generated fields
     */
    @Transactional
    public Book create(BookRequest request) {
        var normalizedRequest = normalizeRequest(request);
        validateReferences(normalizedRequest);
        var id = bookRepository.insert(toDomain(null, normalizedRequest));
        var created = getById(id);
        log.info("Book added: title='{}'", created.title());
        return created;
    }

    /**
     * Updates an existing book after validating referenced shelves and collections.
     *
     * @param id book identifier to update
     * @param request client-supplied replacement payload
     * @return persisted book after the update
     */
    @Transactional
    public Book update(UUID id, BookRequest request) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found: " + id);
        }
        var normalizedRequest = normalizeRequest(request);
        validateReferences(normalizedRequest);
        bookRepository.update(toDomain(id, normalizedRequest));
        return getById(id);
    }

    /**
     * Deletes a book and its collection assignments.
     *
     * @param id unique book identifier
     */
    @Transactional
    public void delete(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found: " + id);
        }
        bookRepository.delete(id);
        log.info("Book deleted: id={}", id);
    }

    private void validateReferences(BookRequest request) {
        if (request.shelfId() != null && !request.shelfId().isBlank() && !shelfRepository.existsById(request.shelfId())) {
            log.warn("Rejected book write due to unknown shelf id '{}'", request.shelfId());
            throw new InvalidReferenceException("Unknown shelf id: " + request.shelfId());
        }
        var collectionIds = request.collectionIds() == null ? List.<String>of() : request.collectionIds();
        var missingCollectionIds = collectionIds.stream()
                .filter(id -> !collectionRepository.existsById(id))
                .distinct()
                .toList();
        if (!missingCollectionIds.isEmpty()) {
            log.warn("Rejected book write due to unknown collection ids {}", missingCollectionIds);
            throw new InvalidReferenceException("Unknown collection ids: " + String.join(", ", missingCollectionIds));
        }
    }

    private BookSearchCriteria normalizeCriteria(BookSearchCriteria criteria) {
        return new BookSearchCriteria(
                trimToNull(criteria.query()),
                normalizeCollectionIds(criteria.collectionIds()),
                trimToNull(criteria.collectionMode()),
                criteria.noCollectionOnly(),
                criteria.noBestEstimateOnly(),
                trimToNull(criteria.shelfId()),
                criteria.year(),
                criteria.hasShelf(),
                criteria.specialOnly(),
                criteria.digitalOnly(),
                criteria.hardbackOnly(),
                criteria.dustjacketOnly(),
                criteria.slipcaseOnly(),
                criteria.videoOnly(),
                trimToNull(criteria.dateAddedMode()),
                criteria.dateAddedOn(),
                criteria.dateAddedYear(),
                trimToNull(criteria.lastAuditedMode()),
                criteria.lastAuditedOn(),
                trimToNull(criteria.sortBy()),
                trimToNull(criteria.sortDirection()),
                criteria.page(),
                criteria.size()
        );
    }

    private BookRequest normalizeRequest(BookRequest request) {
        return new BookRequest(
                trim(request.title()),
                trimToNull(request.author()),
                trimToNull(request.publicationCity()),
                trimToNull(request.publisher()),
                trimToNull(request.imprint()),
                request.year(),
                request.firstPublished(),
                trimToNull(request.edition()),
                request.pages(),
                trimToNull(request.frontpages()),
                request.dateAdded(),
                request.lastAudited(),
                trimToNull(request.shelfId()),
                trimToNull(request.authorSecondary()),
                trimToNull(request.isbn()),
                request.special(),
                request.digital(),
                request.hardback(),
                request.dustjacket(),
                request.slipcase(),
                request.video(),
                request.vols(),
                trimToNull(request.series()),
                trimToNull(request.shelfCoords()),
                trimToNull(request.note()),
                request.pricePaid(),
                request.priceOnItem(),
                request.priceToReplace(),
                request.priceToReplaceChecked(),
                normalizeCollectionIds(request.collectionIds())
        );
    }

    private List<String> normalizeCollectionIds(List<String> collectionIds) {
        if (collectionIds == null) {
            return List.of();
        }
        return collectionIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .flatMap(id -> java.util.Arrays.stream(id.split(",")))
                .map(this::trimToNull)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        var trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private Book toDomain(UUID id, BookRequest request) {
        return new Book(
                id,
                request.title(),
                request.author(),
                request.publicationCity(),
                request.publisher(),
                request.imprint(),
                request.year(),
                request.firstPublished(),
                request.edition(),
                request.pages(),
                request.frontpages(),
                request.dateAdded(),
                request.lastAudited(),
                request.shelfId(),
                request.authorSecondary(),
                request.isbn(),
                Boolean.TRUE.equals(request.special()),
                Boolean.TRUE.equals(request.digital()),
                request.hardback(),
                Boolean.TRUE.equals(request.dustjacket()),
                Boolean.TRUE.equals(request.slipcase()),
                Boolean.TRUE.equals(request.video()),
                request.vols(),
                request.series(),
                request.shelfCoords(),
                request.note(),
                request.pricePaid(),
                request.priceOnItem(),
                request.priceToReplace(),
                request.priceToReplaceChecked(),
                null,
                null,
                request.collectionIds() == null ? List.of() : request.collectionIds()
        );
    }
}
