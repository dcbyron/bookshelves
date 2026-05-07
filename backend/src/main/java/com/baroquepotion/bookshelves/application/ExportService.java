package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.ExportRequest;
import com.baroquepotion.bookshelves.api.dto.ExportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates export requests while delegating environment-specific execution details.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final ExportExecutor exportExecutor;

    public ExportService(ExportExecutor exportExecutor) {
        this.exportExecutor = exportExecutor;
    }

    /**
     * Creates a timestamped export using the project backup script.
     *
     * @param request export options chosen by the user
     * @return completed export metadata
     */
    @Transactional(readOnly = true)
    public ExportResponse createExport(ExportRequest request) {
        log.debug("Delegating export request to configured export executor");
        return exportExecutor.createExport(request);
    }
}
