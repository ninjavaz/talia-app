package app.talia.auth.entity;

import app.talia.auth.model.TokenPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Table {@code email_token}: a one-time token of an e-mail link, stored only as its SHA-256. */
@Entity
@Table(name = "email_token")
public class EmailTokenEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64)
    private String tokenHash;

    private long userId;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose;

    private Instant expiresAt;
    private Instant createdAt;

    protected EmailTokenEntity() {
    }

    public EmailTokenEntity(String tokenHash, long userId, TokenPurpose purpose, Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public long getUserId() {
        return userId;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
