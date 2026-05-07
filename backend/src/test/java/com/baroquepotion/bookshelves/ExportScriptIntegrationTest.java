package com.baroquepotion.bookshelves;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExportScriptIntegrationTest {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Test
    void exportScriptCreatesPortableArtifactsWithCurrentBookFields(@TempDir Path tempDir) throws Exception {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
                .withDatabaseName("bookshelves_export")
                .withUsername("bookshelves")
                .withPassword("bookshelves")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("filesystem:" + repoRoot().resolve("backend/src/main/resources/db/migration").toAbsolutePath())
                    .load()
                    .migrate();

            markShelfAudited(postgres);
            markShelfCoordinateAudited(postgres);
            insertSampleBook(postgres);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    repoRoot().resolve("scripts/export_bookshelves.sh").toString(),
                    "--portable-only",
                    "--out",
                    tempDir.toString()
            );
            processBuilder.directory(repoRoot().toFile());
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("BOOKSHELVES_DB_HOST", postgres.getHost());
            processBuilder.environment().put("BOOKSHELVES_DB_PORT", String.valueOf(postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
            processBuilder.environment().put("BOOKSHELVES_DB_NAME", postgres.getDatabaseName());
            processBuilder.environment().put("BOOKSHELVES_DB_USERNAME", postgres.getUsername());
            processBuilder.environment().put("BOOKSHELVES_DB_PASSWORD", postgres.getPassword());

            Process process = processBuilder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().reduce("", (left, right) -> left + right + System.lineSeparator());
            }

            assertThat(process.waitFor()).isZero();
            assertThat(output).contains("Export written to: ");

            Path exportDir = Path.of(output.lines()
                    .filter(line -> line.startsWith("Export written to: "))
                    .findFirst()
                    .orElseThrow()
                    .substring("Export written to: ".length())
                    .trim());
            Path manifestPath = exportDir.resolve("manifest.json");
            Path bookJsonlPath = exportDir.resolve("book.jsonl");
            Path shelfJsonlPath = exportDir.resolve("shelf.jsonl");
            Path shelfCoordinateAuditJsonlPath = exportDir.resolve("shelf_coordinate_audit.jsonl");

            assertThat(manifestPath).exists();
            assertThat(bookJsonlPath).exists();
            assertThat(shelfJsonlPath).exists();
            assertThat(shelfCoordinateAuditJsonlPath).exists();

            Map<String, Object> manifest = new ObjectMapper().readValue(Files.readString(manifestPath), MAP_TYPE);
            assertThat((Map<String, Object>) manifest.get("includes"))
                    .containsEntry("portableJsonl", true)
                    .containsEntry("postgresDump", false);
            assertThat((Map<String, Object>) manifest.get("rowCounts"))
                    .containsEntry("book", 9)
                    .containsEntry("shelf_coordinate_audit", 1);

            Map<String, Object> book = Files.readAllLines(bookJsonlPath).stream()
                    .map(this::readMap)
                    .filter(row -> "Exported Book".equals(row.get("title")))
                    .findFirst()
                    .orElseThrow();
            assertThat(book)
                    .containsEntry("title", "Exported Book")
                    .containsEntry("last_audited", "2026-04-01")
                    .containsEntry("digital", true);

            Map<String, Object> shelf = Files.readAllLines(shelfJsonlPath).stream()
                    .map(line -> readMap(line))
                    .filter(row -> "uncategorized".equals(row.get("id")))
                    .findFirst()
                    .orElseThrow();
            assertThat(shelf).containsEntry("audited", true);

            Map<String, Object> shelfCoordinateAudit = Files.readAllLines(shelfCoordinateAuditJsonlPath).stream()
                    .map(line -> readMap(line))
                    .filter(row -> "uncategorized".equals(row.get("shelf_id")))
                    .findFirst()
                    .orElseThrow();
            assertThat(shelfCoordinateAudit)
                    .containsEntry("shelf_coords", "A-1")
                    .containsEntry("audited", true);
        }
    }

    private Map<String, Object> readMap(String line) {
        try {
            return new ObjectMapper().readValue(line, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse JSONL row", exception);
        }
    }

    private void markShelfAudited(PostgreSQLContainer postgres) throws Exception {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             PreparedStatement statement = connection.prepareStatement("update shelf set audited = true where id = ?")) {
            statement.setString(1, "uncategorized");
            statement.executeUpdate();
        }
    }

    private void markShelfCoordinateAudited(PostgreSQLContainer postgres) throws Exception {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             PreparedStatement statement = connection.prepareStatement("""
                     insert into shelf_coordinate_audit (shelf_id, shelf_coords, audited)
                     values (?, ?, true)
                     """)) {
            statement.setString(1, "uncategorized");
            statement.setString(2, "A-1");
            statement.executeUpdate();
        }
    }

    private void insertSampleBook(PostgreSQLContainer postgres) throws Exception {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    insert into book (
                        id, title, author, publication_city, publisher, year, edition, pages, frontpages, date_added, last_audited,
                        author_secondary, isbn, special, digital, hardback, vols, series, shelf_coords, note,
                        price_paid, price_on_item, price_to_replace, price_to_replace_checked
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setObject(1, UUID.randomUUID());
                statement.setString(2, "Exported Book");
                statement.setString(3, "Casey Cataloger");
                statement.setString(4, "New York");
                statement.setString(5, "Bookshelves Press");
                statement.setInt(6, 2026);
                statement.setString(7, "1");
                statement.setInt(8, 123);
                statement.setString(9, "x");
                statement.setObject(10, LocalDate.of(2026, 3, 31));
                statement.setObject(11, LocalDate.of(2026, 4, 1));
                statement.setString(12, "Pat Proofreader");
                statement.setString(13, "9781234567890");
                statement.setBoolean(14, true);
                statement.setBoolean(15, true);
                statement.setBoolean(16, false);
                statement.setInt(17, 1);
                statement.setString(18, "Testing Series");
                statement.setString(19, "A-1");
                statement.setString(20, "export integration test");
                statement.setBigDecimal(21, BigDecimal.valueOf(10.50));
                statement.setBigDecimal(22, BigDecimal.valueOf(12.00));
                statement.setBigDecimal(23, BigDecimal.valueOf(14.25));
                statement.setObject(24, LocalDate.of(2026, 4, 2));
                statement.executeUpdate();
            }
        }
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("scripts/export_bookshelves.sh"))) {
            return cwd;
        }
        if (cwd.getParent() != null && Files.exists(cwd.getParent().resolve("scripts/export_bookshelves.sh"))) {
            return cwd.getParent();
        }
        throw new IllegalStateException("Could not locate repo root from " + cwd);
    }
}
