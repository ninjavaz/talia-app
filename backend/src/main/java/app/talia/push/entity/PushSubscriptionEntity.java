package app.talia.push.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Table {@code push_subscription}: one browser/device that receives the user's push notifications. */
@Entity
@Table(name = "push_subscription")
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long userId;

    /** Push service URL of the device (validated against the allowed push services). */
    private String endpoint;

    /** Device public key (base64url, uncompressed P-256 point) and authentication secret (base64url, 16 bytes). */
    private String p256dh;
    private String auth;

    private Instant createdAt;
    private Instant lastUsedAt;

    protected PushSubscriptionEntity() {
    }

    public PushSubscriptionEntity(long userId, String endpoint, String p256dh, String auth, Instant createdAt) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = createdAt;
    }

    /** The same device subscribed again (possibly after logging in to another account). */
    public void renew(long userId, String p256dh, String auth) {
        this.userId = userId;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
