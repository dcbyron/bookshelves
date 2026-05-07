package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.ExportRequest;
import com.baroquepotion.bookshelves.api.dto.ExportResponse;

/**
 * Strategy for producing backup/export artifacts for the catalog.
 */
public interface ExportExecutor {

    /**
     * Creates a timestamped export and returns its manifest-backed metadata.
     *
     * @param request export options chosen by the caller
     * @return export metadata describing the generated files
     */
    ExportResponse createExport(ExportRequest request);
}
