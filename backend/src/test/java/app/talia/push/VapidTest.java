package app.talia.push;

import app.talia.push.service.EcKeys;
import app.talia.push.service.Vapid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** VAPID (RFC 8292): the Authorization header carries an ES256 JWT for the push service's origin. */
class VapidTest {

    private static final Pattern HEADER = Pattern.compile("vapid t=([\\w-]+)\\.([\\w-]+)\\.([\\w-]+), k=([\\w-]+)");

    @Test
    @DisplayName("REQ-033 the VAPID token is signed with the server key and names the push service")
    void signedTokenForThePushService() throws Exception {
        Vapid vapid = Vapid.generate("mailto:admin@example.org");
        Instant now = Instant.parse("2026-11-15T09:00:00Z");

        String header = vapid.authorization("https://fcm.googleapis.com/fcm/send/abc:def", now);

        Matcher m = HEADER.matcher(header);
        assertThat(m.matches()).as(header).isTrue();
        assertThat(m.group(4)).isEqualTo(vapid.publicKey());
        String claims = new String(Base64.getUrlDecoder().decode(m.group(2)), StandardCharsets.UTF_8);
        assertThat(claims)
                .contains("\"aud\":\"https://fcm.googleapis.com\"")
                .contains("\"sub\":\"mailto:admin@example.org\"")
                .contains("\"exp\":" + now.plusSeconds(12 * 3600).getEpochSecond());
        assertThat(new String(Base64.getUrlDecoder().decode(m.group(1)), StandardCharsets.UTF_8))
                .contains("\"alg\":\"ES256\"");

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(EcKeys.publicKey(Base64.getUrlDecoder().decode(m.group(4))));
        verifier.update((m.group(1) + "." + m.group(2)).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(m.group(3)))).isTrue();
    }

    @Test
    @DisplayName("REQ-033 configured VAPID keys are used as given")
    void configuredKeys() {
        java.security.KeyPair pair = EcKeys.generate();
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String publicKey = b64.encodeToString(EcKeys.raw(pair));
        String privateKey = b64.encodeToString(EcKeys.rawPrivate(pair));

        Vapid configured = Vapid.of(publicKey, privateKey, "mailto:a@example.org");

        assertThat(configured.publicKey()).isEqualTo(publicKey);
    }
}
