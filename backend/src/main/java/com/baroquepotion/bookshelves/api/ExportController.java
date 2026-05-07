package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.api.dto.ExportRequest;
import com.baroquepotion.bookshelves.api.dto.ExportResponse;
import com.baroquepotion.bookshelves.application.ExportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers timestamped backup/export runs for the local PostgreSQL catalog database.
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Creates a new export run according to the supplied options.
     *
     * @param request export destination and format options
     * @return metadata describing the completed export
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExportResponse create(@RequestBody(required = false) ExportRequest request) {
        ExportRequest value = request == null ? new ExportRequest(null, true, true, false) : request;
        return exportService.createExport(value);
    }
}
