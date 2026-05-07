package com.baroquepotion.bookshelves.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes runtime configuration needed by the web frontend.
 */
@RestController
@Tag(name = "Configuration")
public class ConfigController {

    private final String appName;

    public ConfigController(@Value("${bookshelves.app-name:Bookshelves}") String appName) {
        this.appName = appName;
    }

    /**
     * Returns public frontend configuration.
     *
     * @return application display settings
     */
    @GetMapping("/api/config")
    @Operation(summary = "Frontend configuration")
    public Map<String, String> config() {
        return Map.of("appName", appName);
    }
}
