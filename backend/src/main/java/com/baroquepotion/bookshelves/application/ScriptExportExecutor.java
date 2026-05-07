package com.baroquepotion.bookshelves.application;

import com.baroquepotion.bookshelves.api.dto.ExportRequest;
import com.baroquepotion.bookshelves.api.dto.ExportResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes the shared project export script with environment-aware configuration.
 */
@Component
public class ScriptExportExecutor implements ExportExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScriptExportExecutor.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Pattern POSTGRES_JDBC_URL =
            Pattern.compile("^jdbc:postgresql://(?<host>[^/:]+)(?::(?<port>\\d+))?/(?<database>[^?]+).*$");

    private final ObjectMapper objectMapper;
    private final ExportScriptProperties properties;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;

    public ScriptExportExecutor(ObjectMapper objectMapper,
                                ExportScriptProperties properties,
                                @Value("${spring.datasource.url}") String datasourceUrl,
                                @Value("${spring.datasource.username}") String datasourceUsername,
                                @Value("${spring.datasource.password}") String datasourcePassword) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
    }

    @Override
    public ExportResponse createExport(ExportRequest request) {
        ExportOptions options = normalizeOptions(request);

        if (!options.portableJsonl() && !options.postgresDump()) {
            throw new IllegalArgumentException("Choose at least one export type.");
        }

        Path scriptPath = resolveScriptPath();
        Path workingDirectory = resolveWorkingDirectory(scriptPath);
        List<String> command = buildCommand(scriptPath, options);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);
        configureEnvironment(processBuilder.environment());
        log.info("Starting export with portableJsonl={}, postgresDump={}, compress={}, outputDirectory={}",
                options.portableJsonl(), options.postgresDump(), options.compress(),
                options.outputDirectory().orElse("default"));

        try {
            Process process = processBuilder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().reduce("", (left, right) -> left + right + System.lineSeparator());
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(output.isBlank() ? "Export failed." : output.strip());
            }

            String exportDirectory = parseExportDirectory(output);
            Path manifestPath = Paths.get(exportDirectory).resolve("manifest.json");
            Map<String, Object> manifest = objectMapper.readValue(Files.readString(manifestPath), MAP_TYPE);
            log.info("Export completed: directory={}, manifest={}", exportDirectory, manifestPath);

            return new ExportResponse(
                    exportDirectory,
                    manifestPath.toString(),
                    String.valueOf(manifest.get("exportedAt")),
                    String.valueOf(manifest.get("schemaVersion")),
                    castMap(manifest.get("includes")),
                    castMap(manifest.get("rowCounts")),
                    castFiles(manifest.get("files"))
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Export interrupted", ex);
            throw new IllegalStateException("Export was interrupted.", ex);
        } catch (IOException ex) {
            log.error("Export failed while reading script output", ex);
            throw new IllegalStateException("Export failed to read script output.", ex);
        } catch (RuntimeException ex) {
            log.error("Export failed", ex);
            throw ex;
        }
    }

    private ExportOptions normalizeOptions(ExportRequest request) {
        Objects.requireNonNull(request, "Export request is required");
        return new ExportOptions(
                normalizeOptionalText(request.outputDirectory()),
                request.portableJsonl() == null || request.portableJsonl(),
                request.postgresDump() == null || request.postgresDump(),
                request.compress() != null && request.compress());
    }

    private List<String> buildCommand(Path scriptPath, ExportOptions options) {
        List<String> command = new ArrayList<>();
        command.add(scriptPath.toString());
        options.outputDirectory().ifPresent(outputDirectory -> {
            command.add("--out");
            command.add(outputDirectory);
        });
        if (options.portableJsonl() && !options.postgresDump()) {
            command.add("--portable-only");
        } else if (!options.portableJsonl()) {
            command.add("--dump-only");
        }
        if (options.compress()) {
            command.add("--compress");
        }
        return command;
    }

    private Path resolveScriptPath() {
        Optional<String> configuredPath = normalizeOptionalText(properties.getPath());
        if (configuredPath.isPresent()) {
            Path configured = Paths.get(configuredPath.get()).toAbsolutePath().normalize();
            if (Files.exists(configured)) {
                return configured;
            }
            throw new IllegalStateException("Configured export script does not exist: " + configured);
        }

        Path cwd = Paths.get("").toAbsolutePath();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve("scripts").resolve("export_bookshelves.sh"));
        Optional.ofNullable(cwd.getParent())
                .map(parent -> parent.resolve("scripts").resolve("export_bookshelves.sh"))
                .ifPresent(candidates::add);
        Optional.ofNullable(cwd.getParent())
                .map(Path::getParent)
                .map(grandparent -> grandparent.resolve("scripts").resolve("export_bookshelves.sh"))
                .ifPresent(candidates::add);
        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not locate scripts/export_bookshelves.sh"));
    }

    private Path resolveWorkingDirectory(Path scriptPath) {
        Optional<String> configuredWorkingDirectory = normalizeOptionalText(properties.getWorkingDirectory());
        if (configuredWorkingDirectory.isPresent()) {
            Path configured = Paths.get(configuredWorkingDirectory.get()).toAbsolutePath().normalize();
            if (Files.exists(configured)) {
                return configured;
            }
            throw new IllegalStateException("Configured export working directory does not exist: " + configured);
        }
        return scriptPath.getParent().getParent();
    }

    private void configureEnvironment(Map<String, String> environment) {
        environment.putIfAbsent("BOOKSHELVES_DB_USERNAME", datasourceUsername);
        environment.putIfAbsent("BOOKSHELVES_DB_PASSWORD", datasourcePassword);
        JdbcConnectionInfo info = parseDatasourceUrl(datasourceUrl);
        environment.putIfAbsent("BOOKSHELVES_DB_HOST", info.host());
        environment.putIfAbsent("BOOKSHELVES_DB_PORT", info.port());
        environment.putIfAbsent("BOOKSHELVES_DB_NAME", info.database());
    }

    private JdbcConnectionInfo parseDatasourceUrl(String jdbcUrl) {
        Matcher matcher = POSTGRES_JDBC_URL.matcher(normalizeOptionalText(jdbcUrl).orElse(""));
        if (!matcher.matches()) {
            throw new IllegalStateException("Unsupported PostgreSQL JDBC URL for export: " + jdbcUrl);
        }
        String port = matcher.group("port");
        return new JdbcConnectionInfo(
                matcher.group("host"),
                port == null || port.isBlank() ? "5432" : port,
                matcher.group("database")
        );
    }

    private Optional<String> normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private String parseExportDirectory(String output) {
        return output.lines()
                .filter(line -> line.startsWith("Export written to: "))
                .map(line -> line.substring("Export written to: ".length()).trim())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Export completed without reporting its output directory."));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castFiles(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private record JdbcConnectionInfo(String host, String port, String database) {
    }

    private record ExportOptions(Optional<String> outputDirectory,
                                 boolean portableJsonl,
                                 boolean postgresDump,
                                 boolean compress) {
    }
}
