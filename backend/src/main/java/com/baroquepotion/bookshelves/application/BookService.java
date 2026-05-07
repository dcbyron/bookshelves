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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        var collectionIds = Objects.requireNonNullElse(request.collectionIds(), List.<String>of());
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
        Objects.requireNonNull(criteria, "Search criteria are required");
        return new BookSearchCriteria(
                optionalText(criteria.query()),
                normalizeCollectionIds(criteria.collectionIds()),
                optionalText(criteria.collectionMode()),
                criteria.noCollectionOnly(),
                criteria.noBestEstimateOnly(),
                optionalText(criteria.shelfId()),
                criteria.year(),
                criteria.hasShelf(),
                criteria.specialOnly(),
                criteria.digitalOnly(),
                criteria.hardbackOnly(),
                criteria.dustjacketOnly(),
                criteria.slipcaseOnly(),
                criteria.videoOnly(),
                optionalText(criteria.dateAddedMode()),
                criteria.dateAddedOn(),
                criteria.dateAddedYear(),
                optionalText(criteria.lastAuditedMode()),
                criteria.lastAuditedOn(),
                optionalText(criteria.sortBy()),
                optionalText(criteria.sortDirection()),
                criteria.page(),
                criteria.size()
        );
    }

    private BookRequest normalizeRequest(BookRequest request) {
        Objects.requireNonNull(request, "Book request is required");
        return new BookRequest(
                requiredText(request.title(), "Title"),
                optionalText(request.author()),
                optionalText(request.publicationCity()),
                optionalText(request.publisher()),
                optionalText(request.imprint()),
                request.year(),
                request.firstPublished(),
                optionalText(request.edition()),
                request.pages(),
                optionalText(request.frontpages()),
                request.dateAdded(),
                request.lastAudited(),
                optionalText(request.shelfId()),
                optionalText(request.authorSecondary()),
                optionalText(request.isbn()),
                request.special(),
                request.digital(),
                request.hardback(),
                request.dustjacket(),
                request.slipcase(),
                request.video(),
                request.vols(),
                optionalText(request.series()),
                optionalText(request.shelfCoords()),
                optionalText(request.note()),
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
                .filter(Objects::nonNull)
                .flatMap(id -> Arrays.stream(id.split(",")))
                .map(this::normalizedText)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    private String requiredText(String value, String label) {
        return normalizedText(value)
                .orElseThrow(() -> new IllegalArgumentException(label + " must not be blank"));
    }

    private String optionalText(String value) {
        return normalizedText(value).orElse(null);
    }

    private Optional<String> normalizedText(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
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
                request.collectionIds()
        );
    }
}
