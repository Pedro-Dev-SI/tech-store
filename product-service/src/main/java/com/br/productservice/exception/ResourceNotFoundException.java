package com.br.productservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String identifier) {
      super(String.format("%s not found with the identifier: %s", resource, identifier));
    }
}
