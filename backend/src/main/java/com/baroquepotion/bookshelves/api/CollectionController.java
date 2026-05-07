package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.CollectionRequest;
import com.baroquepotion.bookshelves.api.dto.CollectionResponse;
import com.baroquepotion.bookshelves.application.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for managing named book collections.
 */
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * Returns all collection definitions ordered for display.
     *
     * @return known collections
     */
    @GetMapping
    public List<CollectionResponse> list() {
        return collectionService.list().stream()
                .map(collection -> new CollectionResponse(collection.id(), collection.name(), collection.usageCount()))
                .toList();
    }

    /**
     * Creates a new collection vocabulary entry.
     *
     * @param request validated collection payload
     * @return created collection
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionResponse create(@Valid @RequestBody CollectionRequest request) {
        var collection = collectionService.create(request);
        return new CollectionResponse(collection.id(), collection.name(), collection.usageCount());
    }

    /**
     * Updates an existing collection definition.
     *
     * @param id collection identifier to replace
     * @param request validated replacement payload
     * @return updated collection
     */
    @PutMapping("/{id}")
    public CollectionResponse update(@PathVariable String id, @Valid @RequestBody CollectionRequest request) {
        var collection = collectionService.update(id, request);
        return new CollectionResponse(collection.id(), collection.name(), collection.usageCount());
    }

    /**
     * Deletes a collection definition.
     *
     * @param id collection identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        collectionService.delete(id);
    }
}
