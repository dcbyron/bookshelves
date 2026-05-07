package com.baroquepotion.bookshelves.application;

/**
 * Raised when a requested domain object cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a human-readable lookup failure message.
     *
     * @param message lookup detail for API callers and logs
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
