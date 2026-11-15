package app.talia.push.model;

/** The server's VAPID public key (base64url) for {@code PushManager.subscribe({applicationServerKey})}. */
public record PushKey(String publicKey) {
}
