package app.talia.push;

import app.talia.config.TaliaProperties;
import app.talia.push.entity.PushSubscriptionEntity;
import app.talia.push.model.PushMessage;
import app.talia.push.model.SendResult;
import app.talia.push.service.EcKeys;
import app.talia.push.service.Vapid;
import app.talia.push.service.WebPushClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** The Web Push HTTP request, sent to a local server that plays the push service. */
class WebPushClientTest {

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(201);
    private final AtomicReference<Map<String, List<String>>> headers = new AtomicReference<>();
    private final AtomicReference<byte[]> body = new AtomicReference<>();
    private final AtomicInteger requests = new AtomicInteger();

    private final KeyPair device = EcKeys.generate();
    private final byte[] auth = Base64.getUrlDecoder().decode("BTBZMqHH6r4Tts7J_aSIgg");
    private final Vapid vapid = Vapid.generate("mailto:admin@example.org");
    private WebPushClient client;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/push/", exchange -> {
            requests.incrementAndGet();
            headers.set(Map.copyOf(exchange.getRequestHeaders()));
            body.set(exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
        TaliaProperties.Push push = new TaliaProperties.Push(null, null, "mailto:admin@example.org",
                List.of("localhost"), true, 10);
        client = new WebPushClient(vapid, push, JsonMapper.builder().build(),
                Clock.fixed(Instant.parse("2026-11-15T09:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private PushSubscriptionEntity subscription(String endpoint) {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return new PushSubscriptionEntity(1, endpoint, b64.encodeToString(EcKeys.raw(device)), b64.encodeToString(auth),
                Instant.now());
    }

    private String endpoint() {
        return "http://localhost:" + server.getAddress().getPort() + "/push/abc";
    }

    private static final PushMessage MESSAGE =
            new PushMessage("Talia", "Pora na: Witamina D", "/week", "habit-4", Duration.ofHours(1));

    @Test
    @DisplayName("REQ-030 the notification is sent encrypted, with VAPID and TTL, in the service worker format")
    void sendsEncryptedNotification() throws Exception {
        assertThat(client.send(subscription(endpoint()), MESSAGE)).isEqualTo(SendResult.OK);

        Map<String, List<String>> h = headers.get();
        assertThat(h.get("Content-encoding")).containsExactly("aes128gcm");
        assertThat(h.get("Ttl")).containsExactly("3600");
        assertThat(h.get("Authorization").getFirst()).startsWith("vapid t=").endsWith(", k=" + vapid.publicKey());

        String payload = new String(decrypt(body.get()), StandardCharsets.UTF_8);
        assertThat(payload)
                .contains("\"notification\"")
                .contains("\"title\":\"Talia\"")
                .contains("\"body\":\"Pora na: Witamina D\"")
                .contains("\"tag\":\"habit-4\"")
                .contains("\"operation\":\"navigateLastFocusedOrOpen\"")
                .contains("\"url\":\"/week\"");
    }

    @Test
    @DisplayName("REQ-033 a subscription the push service no longer knows is reported as gone")
    void goneSubscription() {
        status.set(410);
        assertThat(client.send(subscription(endpoint()), MESSAGE)).isEqualTo(SendResult.GONE);
        status.set(404);
        assertThat(client.send(subscription(endpoint()), MESSAGE)).isEqualTo(SendResult.GONE);
        status.set(500);
        assertThat(client.send(subscription(endpoint()), MESSAGE)).isEqualTo(SendResult.FAILED);
    }

    @Test
    @DisplayName("REQ-033 nothing is sent to an endpoint outside the allowed push services")
    void disallowedEndpointNotRequested() {
        assertThat(client.send(subscription("http://127.0.0.1:" + server.getAddress().getPort() + "/push/abc"), MESSAGE))
                .isEqualTo(SendResult.FAILED);
        assertThat(requests.get()).isZero();
    }

    /** RFC 8291 decryption with the device's private key (independent of the production code). */
    private byte[] decrypt(byte[] message) throws Exception {
        byte[] salt = Arrays.copyOfRange(message, 0, 16);
        int keyLength = message[20];
        byte[] senderPublic = Arrays.copyOfRange(message, 21, 21 + keyLength);
        byte[] ciphertext = Arrays.copyOfRange(message, 21 + keyLength, message.length);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(device.getPrivate());
        agreement.doPhase(EcKeys.publicKey(senderPublic), true);
        byte[] ecdh = agreement.generateSecret();

        byte[] prkKey = hmac(auth, ecdh);
        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), EcKeys.raw(device), senderPublic);
        byte[] ikm = Arrays.copyOf(hmac(prkKey, concat(keyInfo, new byte[]{1})), 32);
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, concat("Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII),
                new byte[]{1})), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, concat("Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII),
                new byte[]{1})), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);
        assertThat(padded[padded.length - 1]).isEqualTo((byte) 2);
        return Arrays.copyOf(padded, padded.length - 1);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        byte[] out = new byte[Arrays.stream(parts).mapToInt(p -> p.length).sum()];
        int offset = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, offset, p.length);
            offset += p.length;
        }
        return out;
    }
}
