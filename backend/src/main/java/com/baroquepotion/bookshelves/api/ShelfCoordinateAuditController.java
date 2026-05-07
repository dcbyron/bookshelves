package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.ShelfCoordinateAuditRequest;
import com.baroquepotion.bookshelves.api.dto.ShelfCoordinateAuditResponse;
import com.baroquepotion.bookshelves.application.ShelfService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for auditing occupied shelf coordinate tiers.
 */
@RestController
@RequestMapping("/api/shelf-coordinate-audits")
public class ShelfCoordinateAuditController {

    private final ShelfService shelfService;

    public ShelfCoordinateAuditController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    /**
     * Returns occupied shelf coordinates and their audit state.
     *
     * @return coordinate audit rows
     */
    @GetMapping
    public List<ShelfCoordinateAuditResponse> list() {
        return shelfService.listCoordinateAudits().stream()
                .map(row -> new ShelfCoordinateAuditResponse(row.shelfId(), row.shelfName(), row.shelfCoords(), row.audited()))
                .toList();
    }

    /**
     * Updates the audit state for one occupied shelf coordinate.
     *
     * @param id shelf identifier
     * @param shelfCoords coordinate tier
     * @param request audit payload
     * @return updated coordinate audit row
     */
    @PutMapping("/{id}/{shelfCoords}")
    public ShelfCoordinateAuditResponse update(
            @PathVariable String id,
            @PathVariable String shelfCoords,
            @RequestBody ShelfCoordinateAuditRequest request) {
        var row = shelfService.updateCoordinateAudit(id, shelfCoords, Boolean.TRUE.equals(request.audited()));
        return new ShelfCoordinateAuditResponse(row.shelfId(), row.shelfName(), row.shelfCoords(), row.audited());
    }
}
