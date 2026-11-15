package app.talia.push.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * VAPID (RFC 8292): identifies this server to the push services with an ES256-signed JWT per request.
 * Keys are the raw base64url forms browsers use ({@code applicationServerKey}).
 */
public final class Vapid {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(12);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER = B64.encodeToString(
            "{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));

    private final KeyPair keyPair;
    private final String publicKey;
    private final String subject;

    private Vapid(KeyPair keyPair, String subject) {
        this.keyPair = keyPair;
        this.publicKey = B64.encodeToString(EcKeys.raw(keyPair));
        this.subject = subject;
    }

    /** Keys from the configuration (base64url: 65-byte public point, 32-byte private scalar). */
    public static Vapid of(String publicKey, String privateKey, String subject) {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        KeyPair pair = new KeyPair(EcKeys.publicKey(decoder.decode(publicKey.trim())),
                EcKeys.privateKey(decoder.decode(privateKey.trim())));
        Vapid vapid = new Vapid(pair, subject);
        if (!vapid.publicKey.equals(publicKey.trim())) {
            throw new IllegalArgumentException("VAPID public key is not in the raw base64url form");
        }
        return vapid;
    }

    /** A new random key pair (local development without configured keys). */
    public static Vapid generate(String subject) {
        return new Vapid(EcKeys.generate(), subject);
    }

    /** The public key for the browser's {@code applicationServerKey}. */
    public String publicKey() {
        return publicKey;
    }

    KeyPair keyPair() {
        return keyPair;
    }

    /** Value of the {@code Authorization} header for a request to {@code endpoint}. */
    public String authorization(String endpoint, Instant now) {
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getAuthority();
        String claims = "{\"aud\":\"" + audience + "\",\"exp\":" + now.plus(TOKEN_LIFETIME).getEpochSecond()
                + ",\"sub\":\"" + subject + "\"}";
        String unsigned = HEADER + "." + B64.encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        try {
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(keyPair.getPrivate());
            signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return "vapid t=" + unsigned + "." + B64.encodeToString(signer.sign()) + ", k=" + publicKey;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("VAPID signing failed", e);
        }
    }
}
