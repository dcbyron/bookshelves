package com.baroquepotion.bookshelves;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Bookshelves backend.
 */
@SpringBootApplication
public class BookshelvesApplication {

    /**
     * Starts the REST API and supporting Spring infrastructure.
     *
     * @param args standard JVM application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BookshelvesApplication.class, args);
    }
}
