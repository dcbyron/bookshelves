package com.baroquepotion.bookshelves.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Describes a completed export created by the backup workflow.
 *
 * @param exportDirectory timestamped export directory created by the script
 * @param manifestPath path to the generated manifest file
 * @param exportedAt UTC timestamp encoded into the export directory name
 * @param schemaVersion Flyway schema version recorded by the export
 * @param includes which artifact types were included
 * @param rowCounts exported row counts by table
 * @param files files created in the export directory
 */
public record ExportResponse(
        String exportDirectory,
        String manifestPath,
        String exportedAt,
        String schemaVersion,
        Map<String, Object> includes,
        Map<String, Object> rowCounts,
        List<Map<String, Object>> files
) {
}
