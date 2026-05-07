package com.baroquepotion.bookshelves.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes a lightweight health endpoint shared by both backend implementations.
 */
@RestController
@Tag(name = "Health")
public class HealthController {

    /**
     * Returns a minimal service health payload.
     *
     * @return static health response
     */
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
