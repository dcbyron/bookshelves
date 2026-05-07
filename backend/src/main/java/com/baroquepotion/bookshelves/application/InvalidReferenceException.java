package com.baroquepotion.bookshelves.application;

/**
 * Raised when a request refers to a collection or shelf that does not exist.
 */
public class InvalidReferenceException extends RuntimeException {

    /**
     * Creates the exception with a human-readable validation message.
     *
     * @param message validation detail for API callers and logs
     */
    public InvalidReferenceException(String message) {
        super(message);
    }
}
