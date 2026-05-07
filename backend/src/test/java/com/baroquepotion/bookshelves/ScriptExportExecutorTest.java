package com.baroquepotion.bookshelves;

import com.baroquepotion.bookshelves.api.dto.ExportRequest;
import com.baroquepotion.bookshelves.api.dto.ExportResponse;
import com.baroquepotion.bookshelves.application.ExportScriptProperties;
import com.baroquepotion.bookshelves.application.ScriptExportExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptExportExecutorTest {

    @TempDir
    Path tempDir;

    @Test
    void createExportPassesEnvironmentAndParsesManifest() throws IOException {
        Path script = writeExecutableScript("""
                #!/usr/bin/env bash
                set -euo pipefail
                work_dir=%s
                output_base=""
                : > "$work_dir/args.txt"
                while [[ $# -gt 0 ]]; do
                  case "$1" in
                    --out)
                      output_base="$2"
                      shift 2
                      ;;
                    *)
                      printf '%%s\\n' "$1" >> "$work_dir/args.txt"
                      shift
                      ;;
                  esac
                done
                export_dir="$output_base/2026-04-09T00-00-00Z"
                mkdir -p "$export_dir"
                printf 'host=%%s\\nport=%%s\\ndatabase=%%s\\nuser=%%s\\n' \\
                  "$BOOKSHELVES_DB_HOST" "$BOOKSHELVES_DB_PORT" "$BOOKSHELVES_DB_NAME" "$BOOKSHELVES_DB_USERNAME" > "$work_dir/env.txt"
                cat > "$export_dir/manifest.json" <<'JSON'
                {
                  "exportedAt": "2026-04-09T00-00-00Z",
                  "schemaVersion": "4",
                  "includes": {"portableJsonl": true, "postgresDump": false},
                  "rowCounts": {"book": 2, "collection": 1, "shelf": 1, "book_collection": 3},
                  "files": [{"name": "book.jsonl", "format": "jsonl", "sizeBytes": 123}]
                }
                JSON
                echo "Export written to: $export_dir"
                """.formatted(tempDir.toString()));

        ExportScriptProperties properties = new ExportScriptProperties();
        properties.setPath(script.toString());
        properties.setWorkingDirectory(tempDir.toString());

        ScriptExportExecutor executor = new ScriptExportExecutor(
                new ObjectMapper(),
                properties,
                "jdbc:postgresql://db.internal:55432/bookshelves_test",
                "catalog_user",
                "secret"
        );

        Path outputBase = tempDir.resolve("exports");
        ExportResponse response = executor.createExport(new ExportRequest(outputBase.toString(), true, false, true));

        assertThat(response.exportDirectory()).isEqualTo(outputBase.resolve("2026-04-09T00-00-00Z").toString());
        assertThat(response.schemaVersion()).isEqualTo("4");
        assertThat(response.includes()).containsEntry("portableJsonl", true).containsEntry("postgresDump", false);
        assertThat(response.rowCounts()).containsEntry("book", 2).containsEntry("book_collection", 3);
        assertThat(response.files()).singleElement().satisfies(file ->
                assertThat(file).containsEntry("name", "book.jsonl"));
        assertThat(Files.readString(tempDir.resolve("env.txt")))
                .contains("host=db.internal")
                .contains("port=55432")
                .contains("database=bookshelves_test")
                .contains("user=catalog_user");
        assertThat(Files.readAllLines(tempDir.resolve("args.txt"))).containsExactly("--portable-only", "--compress");
    }

    @Test
    void createExportRejectsEmptyArtifactSelection() {
        ExportScriptProperties properties = new ExportScriptProperties();
        properties.setPath(tempDir.resolve("missing.sh").toString());

        ScriptExportExecutor executor = new ScriptExportExecutor(
                new ObjectMapper(),
                properties,
                "jdbc:postgresql://localhost:5432/bookshelves",
                "bookshelves",
                "bookshelves"
        );

        assertThatThrownBy(() -> executor.createExport(new ExportRequest(null, false, false, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Choose at least one export type");
    }

    @Test
    void createExportFailsWhenScriptDoesNotReportOutputDirectory() throws IOException {
        Path script = writeExecutableScript("""
                #!/usr/bin/env bash
                set -euo pipefail
                echo "Portable JSONL export files created."
                """);

        ExportScriptProperties properties = new ExportScriptProperties();
        properties.setPath(script.toString());
        properties.setWorkingDirectory(tempDir.toString());

        ScriptExportExecutor executor = new ScriptExportExecutor(
                new ObjectMapper(),
                properties,
                "jdbc:postgresql://localhost:5432/bookshelves",
                "bookshelves",
                "bookshelves"
        );

        assertThatThrownBy(() -> executor.createExport(new ExportRequest(tempDir.resolve("exports").toString(), true, false, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("without reporting its output directory");
    }

    private Path writeExecutableScript(String content) throws IOException {
        Path script = tempDir.resolve("export_bookshelves.sh");
        Files.writeString(script, content);
        try {
            Files.setPosixFilePermissions(script, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));
        } catch (UnsupportedOperationException ignored) {
            script.toFile().setExecutable(true);
        }
        return script;
    }
}
