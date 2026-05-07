package com.baroquepotion.bookshelves.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Writes concise startup and shutdown events so operators can identify the active runtime shape.
 */
@Component
public class ApplicationLifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleLogger.class);

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String port;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${bookshelves.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Logs the active runtime shape once the application is ready to serve traffic.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("{} {} started on port {} using database {} with allowed origins {}",
                applicationName,
                implementationVersion(),
                port,
                datasourceUrl,
                allowedOrigins);
    }

    /**
     * Logs a concise shutdown event so operators can correlate the end of a process lifetime.
     */
    @EventListener(ContextClosedEvent.class)
    public void onClosed() {
        log.info("{} {} shutting down", applicationName, implementationVersion());
    }

    private String implementationVersion() {
        Package appPackage = getClass().getPackage();
        String version = appPackage == null ? null : appPackage.getImplementationVersion();
        return version == null ? "dev" : version;
    }
}
