package com.exception;

import java.util.UUID;

public class APIKeyNotFoundException extends RuntimeException {
    public APIKeyNotFoundException(UUID apiKeyId) {
        super("API key not found: " + apiKeyId);
    }
}
