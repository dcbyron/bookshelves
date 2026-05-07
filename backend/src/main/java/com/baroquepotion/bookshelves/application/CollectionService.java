package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.CollectionRequest;
import com.baroquepotion.bookshelves.domain.Collection;
import com.baroquepotion.bookshelves.persistence.CollectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coordinates CRUD operations for collection vocabulary entries.
 */
@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final CollectionRepository collectionRepository;

    public CollectionService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    /**
     * Returns all known collections.
     *
     * @return ordered collection definitions
     */
    @Transactional(readOnly = true)
    public List<Collection> list() {
        return collectionRepository.findAll();
    }

    /**
     * Creates a collection entry.
     *
     * @param request validated collection payload
     * @return created collection
     */
    @Transactional
    public Collection create(CollectionRequest request) {
        var normalizedRequest = normalizeRequest(request);
        collectionRepository.insert(new Collection(normalizedRequest.id(), normalizedRequest.name(), 0));
        return new Collection(normalizedRequest.id(), normalizedRequest.name(), 0);
    }

    /**
     * Updates a collection entry.
     *
     * @param id existing collection identifier
     * @param request validated replacement payload
     * @return updated collection
     */
    @Transactional
    public Collection update(String id, CollectionRequest request) {
        if (!collectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Collection not found: " + id);
        }
        var normalizedRequest = normalizeRequest(request);
        long usageCount = collectionRepository.usageCount(id);
        if (!id.equals(normalizedRequest.id()) && usageCount > 0) {
            log.warn("Rejected collection rename for '{}' because it is still in use by {} book(s)", id, usageCount);
            throw new ResourceInUseException("Collection id " + id + " is still assigned to " + usageCount + " book(s) and cannot be renamed.");
        }
        collectionRepository.update(id, new Collection(normalizedRequest.id(), normalizedRequest.name(), usageCount));
        return new Collection(normalizedRequest.id(), normalizedRequest.name(), usageCount);
    }

    /**
     * Deletes a collection entry.
     *
     * @param id collection identifier
     */
    @Transactional
    public void delete(String id) {
        if (!collectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Collection not found: " + id);
        }
        long usageCount = collectionRepository.usageCount(id);
        if (usageCount > 0) {
            log.warn("Rejected collection delete for '{}' because it is still in use by {} book(s)", id, usageCount);
            throw new ResourceInUseException("Collection " + id + " is still assigned to " + usageCount + " book(s) and cannot be deleted.");
        }
        collectionRepository.delete(id);
        log.info("Collection deleted: id={}", id);
    }

    private CollectionRequest normalizeRequest(CollectionRequest request) {
        return new CollectionRequest(request.id().trim(), request.name().trim());
    }
}
