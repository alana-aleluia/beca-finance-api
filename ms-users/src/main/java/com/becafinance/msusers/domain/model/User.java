package com.becafinance.msusers.domain.model;

import java.time.OffsetDateTime;

public class User {

    private Long id;
    private String name;
    private String email;
    private OffsetDateTime createdAt;

    public User(Long id, String name, String email,
                OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

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