package com.security;

import java.util.UUID;

public record AuthenticatedPrincipal(String subject, UUID tenantId) {
}
