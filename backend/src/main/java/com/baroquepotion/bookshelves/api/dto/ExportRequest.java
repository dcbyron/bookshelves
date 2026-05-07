package com.baroquepotion.bookshelves.api.dto;

/**
 * Requests a timestamped database export under an optional base directory.
 *
 * @param outputDirectory optional base directory beneath which the timestamped export directory is created
 * @param portableJsonl whether to include portable JSONL table exports
 * @param postgresDump whether to include a PostgreSQL custom-format dump
 * @param compress whether to gzip the JSONL files
 */
public record ExportRequest(
        String outputDirectory,
        Boolean portableJsonl,
        Boolean postgresDump,
        Boolean compress
) {
}
