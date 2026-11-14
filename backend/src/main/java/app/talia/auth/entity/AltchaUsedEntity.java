package app.talia.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Table {@code altcha_used}: SHA-256 of the signature of every accepted ALTCHA solution (each is accepted once). */
@Entity
@Table(name = "altcha_used")
public class AltchaUsedEntity {

    @Id
    private String signature;

    private Instant expiresAt;

    protected AltchaUsedEntity() {
    }

    public String getSignature() {
        return signature;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
