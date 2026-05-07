package com.baroquepotion.bookshelves.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for locating and launching the export script in different environments.
 */
@Component
@ConfigurationProperties(prefix = "bookshelves.export.script")
public class ExportScriptProperties {

    private String path;
    private String workingDirectory;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
