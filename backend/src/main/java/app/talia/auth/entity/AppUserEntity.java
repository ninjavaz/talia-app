package app.talia.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Table {@code app_user}: an account. Username and e-mail are stored lowercase (checked by the database). */
@Entity
@Table(name = "app_user")
public class AppUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String passwordHash;

    /** SHA-256 (hex) of the personal Apple Health sync token; the token itself is never stored. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64)
    private String syncTokenHash;

    private Instant createdAt;
    private Instant emailVerifiedAt;

    protected AppUserEntity() {
    }

    public AppUserEntity(String username, String email, String passwordHash, Instant createdAt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSyncTokenHash() {
        return syncTokenHash;
    }

    public void setSyncTokenHash(String syncTokenHash) {
        this.syncTokenHash = syncTokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    /** Keeps the first confirmation time. */
    public void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }
}
