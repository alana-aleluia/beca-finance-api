package com.becafinance.msusers.domain.model;

import java.time.OffsetDateTime;

public class User {

    private Long id;
    private String name;
    private String email;
    private OffsetDateTime createdAt;
    private String passwordHash;

    public User(Long id, String name, String email,
                OffsetDateTime createdAt) {
        this(id, name, email, createdAt, null);
    }

    public User(Long id, String name, String email, OffsetDateTime createdAt, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getPasswordHash() { return passwordHash; }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
