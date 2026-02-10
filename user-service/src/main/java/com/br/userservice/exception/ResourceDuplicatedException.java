package com.br.userservice.exception;

public class ResourceDuplicatedException extends RuntimeException {
    public ResourceDuplicatedException(String message) {
        super(message);
    }

    public ResourceDuplicatedException(String resource, String identifier) {
        super(String.format("Existing %s with the identifier %s on the database", resource, identifier));
    }
}
