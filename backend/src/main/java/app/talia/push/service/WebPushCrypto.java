package app.talia.push.service;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Message encryption for Web Push (RFC 8291) with the "aes128gcm" content coding (RFC 8188), JDK crypto only.
 * The whole message is one record. Verified against the RFC 8291 example in the tests.
 */
public final class WebPushCrypto {

    private static final int RECORD_SIZE = 4096;
    private static final byte[] KEY_INFO = "WebPush: info\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CEK_INFO = "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NONCE_INFO = "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushCrypto() {
    }

    /** Encrypts for a subscription with a fresh sender key pair and salt (the normal case). */
    public static byte[] encrypt(byte[] plaintext, byte[] receiverPublic, byte[] authSecret) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, receiverPublic, authSecret, EcKeys.generate(), salt);
    }

    /** Deterministic variant (given sender key pair and salt), used for the RFC test vector. */
    public static byte[] encrypt(byte[] plaintext, byte[] receiverPublic, byte[] authSecret, KeyPair sender,
                                 byte[] salt) {
        if (plaintext.length > RECORD_SIZE - 16 - 1 - 86) {
            throw new IllegalArgumentException("Push message too long");
        }
        try {
            byte[] senderPublic = EcKeys.raw(sender);

            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(sender.getPrivate());
            agreement.doPhase(EcKeys.publicKey(receiverPublic), true);
            byte[] ecdhSecret = agreement.generateSecret();

            // RFC 8291 section 3.4: combine the ECDH secret with the authentication secret
            byte[] prkKey = hmac(authSecret, ecdhSecret);
            byte[] ikm = expand(prkKey, concat(KEY_INFO, receiverPublic, senderPublic), 32);

            // RFC 8188 section 2.2/2.3: content encryption key and nonce
            byte[] prk = hmac(salt, ikm);
            byte[] cek = expand(prk, CEK_INFO, 16);
            byte[] nonce = expand(prk, NONCE_INFO, 12);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
            // the last (and only) record ends with the padding delimiter 0x02
            byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[]{2}));

            ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + senderPublic.length);
            header.put(salt).putInt(RECORD_SIZE).put((byte) senderPublic.length).put(senderPublic);
            return concat(header.array(), ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Push encryption failed", e);
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    /** HKDF-Expand for at most one block (all lengths here are <= 32). */
    private static byte[] expand(byte[] prk, byte[] info, int length) throws GeneralSecurityException {
        return Arrays.copyOf(hmac(prk, concat(info, new byte[]{1})), length);
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] out = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, offset, part.length);
            offset += part.length;
        }
        return out;
    }
}
