package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.ShelfRequest;
import com.baroquepotion.bookshelves.api.dto.ShelfResponse;
import com.baroquepotion.bookshelves.application.ShelfService;
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
 * REST endpoints for managing the shelf vocabulary used by books.
 */
@RestController
@RequestMapping("/api/shelves")
public class ShelfController {

    private final ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    /**
     * Returns all shelf definitions ordered for display.
     *
     * @return known shelves
     */
    @GetMapping
    public List<ShelfResponse> list() {
        return shelfService.list().stream()
                .map(shelf -> new ShelfResponse(shelf.id(), shelf.name(), shelf.audited(), shelf.usageCount()))
                .toList();
    }

    /**
     * Creates a new shelf definition.
     *
     * @param request validated shelf payload
     * @return created shelf
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfResponse create(@Valid @RequestBody ShelfRequest request) {
        var shelf = shelfService.create(request);
        return new ShelfResponse(shelf.id(), shelf.name(), shelf.audited(), shelf.usageCount());
    }

    /**
     * Updates an existing shelf definition.
     *
     * @param id shelf identifier to replace
     * @param request validated replacement payload
     * @return updated shelf
     */
    @PutMapping("/{id}")
    public ShelfResponse update(@PathVariable String id, @Valid @RequestBody ShelfRequest request) {
        var shelf = shelfService.update(id, request);
        return new ShelfResponse(shelf.id(), shelf.name(), shelf.audited(), shelf.usageCount());
    }

    /**
     * Deletes a shelf definition.
     *
     * @param id shelf identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        shelfService.delete(id);
    }
}
