package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.ShelfRequest;
import com.baroquepotion.bookshelves.domain.Shelf;
import com.baroquepotion.bookshelves.domain.ShelfCoordinateAudit;
import com.baroquepotion.bookshelves.persistence.ShelfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coordinates CRUD operations for shelf vocabulary entries.
 */
@Service
public class ShelfService {

    private static final Logger log = LoggerFactory.getLogger(ShelfService.class);

    private final ShelfRepository shelfRepository;

    public ShelfService(ShelfRepository shelfRepository) {
        this.shelfRepository = shelfRepository;
    }

    /**
     * Returns all known shelves.
     *
     * @return ordered shelf definitions
     */
    @Transactional(readOnly = true)
    public List<Shelf> list() {
        return shelfRepository.findAll();
    }

    /**
     * Returns current occupied shelf coordinates with persisted audit state.
     *
     * @return coordinate audit rows
     */
    @Transactional(readOnly = true)
    public List<ShelfCoordinateAudit> listCoordinateAudits() {
        return shelfRepository.findCoordinateAudits();
    }

    /**
     * Creates a shelf entry.
     *
     * @param request validated shelf payload
     * @return created shelf
     */
    @Transactional
    public Shelf create(ShelfRequest request) {
        var normalizedRequest = normalizeRequest(request);
        shelfRepository.insert(new Shelf(
                normalizedRequest.id(),
                normalizedRequest.name(),
                normalizedRequest.audited(),
                0));
        return new Shelf(normalizedRequest.id(), normalizedRequest.name(), normalizedRequest.audited(), 0);
    }

    /**
     * Updates a shelf entry.
     *
     * @param id existing shelf identifier
     * @param request validated replacement payload
     * @return updated shelf
     */
    @Transactional
    public Shelf update(String id, ShelfRequest request) {
        if (!shelfRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shelf not found: " + id);
        }
        var normalizedRequest = normalizeRequest(request);
        long usageCount = shelfRepository.usageCount(id);
        if (!id.equals(normalizedRequest.id()) && usageCount > 0) {
            log.warn("Rejected shelf rename for '{}' because it is still in use by {} book(s)", id, usageCount);
            throw new ResourceInUseException("Shelf id " + id + " is still assigned to " + usageCount + " book(s) and cannot be renamed.");
        }
        shelfRepository.update(id, new Shelf(
                normalizedRequest.id(),
                normalizedRequest.name(),
                normalizedRequest.audited(),
                usageCount));
        return new Shelf(normalizedRequest.id(), normalizedRequest.name(), normalizedRequest.audited(), usageCount);
    }

    /**
     * Updates one shelf-coordinate audit checkbox.
     *
     * @param shelfId shelf identifier
     * @param shelfCoords shelf coordinate tier
     * @param audited audit state to persist
     * @return updated coordinate audit row
     */
    @Transactional
    public ShelfCoordinateAudit updateCoordinateAudit(String shelfId, String shelfCoords, boolean audited) {
        var normalizedCoords = normalizeShelfCoords(shelfCoords);
        if (!shelfRepository.existsById(shelfId)) {
            throw new ResourceNotFoundException("Shelf not found: " + shelfId);
        }
        shelfRepository.updateCoordinateAudit(shelfId, normalizedCoords, audited);
        return shelfRepository.findCoordinateAudits().stream()
                .filter(row -> row.shelfId().equals(shelfId) && row.shelfCoords().equals(normalizedCoords))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Shelf coordinate not found: " + shelfId + " " + normalizedCoords));
    }

    /**
     * Deletes a shelf entry.
     *
     * @param id shelf identifier
     */
    @Transactional
    public void delete(String id) {
        if (!shelfRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shelf not found: " + id);
        }
        long usageCount = shelfRepository.usageCount(id);
        if (usageCount > 0) {
            log.warn("Rejected shelf delete for '{}' because it is still in use by {} book(s)", id, usageCount);
            throw new ResourceInUseException("Shelf " + id + " is still assigned to " + usageCount + " book(s) and cannot be deleted.");
        }
        shelfRepository.delete(id);
        log.info("Shelf deleted: id={}", id);
    }

    private ShelfRequest normalizeRequest(ShelfRequest request) {
        return new ShelfRequest(request.id().trim(), request.name().trim(), Boolean.TRUE.equals(request.audited()));
    }

    private String normalizeShelfCoords(String shelfCoords) {
        var normalized = shelfCoords == null ? "" : shelfCoords.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Shelf coordinates are required.");
        }
        return normalized;
    }
}
