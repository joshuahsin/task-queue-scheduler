package com.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

enum Tier {
    FREE,
    PREMIUM
}

@Entity
@Table(name = "plans")
public class Plan {
    private UUID id;
    private Tier tier;
    private int rateLimit;
    private int maxRetries;
    private int payloadSizeLimit;
}
