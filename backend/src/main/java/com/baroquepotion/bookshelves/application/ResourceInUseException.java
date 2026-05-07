package com.baroquepotion.bookshelves.application;

/**
 * Raised when a reference record cannot be deleted or renamed because books still depend on it.
 */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
