package com.becafinance.msusers.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String name, String email,
                         OffsetDateTime createdAt, String passwordHash) {
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
