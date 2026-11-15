package app.talia.push;

import app.talia.push.service.EcKeys;
import app.talia.push.service.WebPushCrypto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/** Push message encryption (RFC 8291) against the example of RFC 8291 section 5 / appendix A. */
class WebPushCryptoTest {

    private static byte[] b64(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    @Test
    @DisplayName("REQ-030 push messages are encrypted exactly like the RFC 8291 example")
    void rfc8291TestVector() {
        KeyPair sender = new KeyPair(
                EcKeys.publicKey(b64("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8")),
                EcKeys.privateKey(b64("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));
        byte[] receiverPublic = b64("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] authSecret = b64("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = b64("DGv6ra1nlYgDCS1FRnbzlw");

        byte[] body = WebPushCrypto.encrypt("When I grow up, I want to be a watermelon".getBytes(StandardCharsets.US_ASCII),
                receiverPublic, authSecret, sender, salt);

        assertThat(Base64.getUrlEncoder().withoutPadding().encodeToString(body)).isEqualTo(
                "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27ml"
                        + "mlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPT"
                        + "pK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
    }

    @Test
    @DisplayName("REQ-030 every message gets a fresh key and salt")
    void freshKeyAndSaltPerMessage() {
        KeyPair receiver = EcKeys.generate();
        byte[] receiverPublic = EcKeys.raw(receiver);
        byte[] auth = new byte[16];

        byte[] a = WebPushCrypto.encrypt("hi".getBytes(StandardCharsets.UTF_8), receiverPublic, auth);
        byte[] b = WebPushCrypto.encrypt("hi".getBytes(StandardCharsets.UTF_8), receiverPublic, auth);

        // header: salt (16) | record size (4) | key length (1) | sender key (65), then ciphertext + 16 byte tag
        assertThat(a).hasSize(16 + 4 + 1 + 65 + 2 + 1 + 16);
        assertThat(a).isNotEqualTo(b);
    }
}
