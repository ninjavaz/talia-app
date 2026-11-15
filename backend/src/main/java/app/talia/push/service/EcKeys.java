package app.talia.push.service;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

/**
 * P-256 keys in the raw forms used by Web Push: public key = uncompressed point (0x04 | x | y, 65 bytes),
 * private key = scalar (32 bytes).
 */
public final class EcKeys {

    private static final ECParameterSpec P256 = parameters();

    private EcKeys() {
    }

    public static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ECPublicKey publicKey(byte[] raw) {
        if (raw.length != 65 || raw[0] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed P-256 point");
        }
        ECPoint point = new ECPoint(new BigInteger(1, Arrays.copyOfRange(raw, 1, 33)),
                new BigInteger(1, Arrays.copyOfRange(raw, 33, 65)));
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, P256));
        } catch (GeneralSecurityException e) {
            // also thrown for a point that is not on the curve
            throw new IllegalArgumentException("Invalid P-256 public key", e);
        }
    }

    public static ECPrivateKey privateKey(byte[] raw) {
        if (raw.length != 32) {
            throw new IllegalArgumentException("Not a P-256 private key");
        }
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 private key", e);
        }
    }

    /** Uncompressed public point of the pair. */
    public static byte[] raw(KeyPair pair) {
        ECPoint w = ((ECPublicKey) pair.getPublic()).getW();
        byte[] out = new byte[65];
        out[0] = 0x04;
        copy32(w.getAffineX(), out, 1);
        copy32(w.getAffineY(), out, 33);
        return out;
    }

    public static byte[] rawPrivate(KeyPair pair) {
        byte[] out = new byte[32];
        copy32(((ECPrivateKey) pair.getPrivate()).getS(), out, 0);
        return out;
    }

    private static void copy32(BigInteger value, byte[] out, int offset) {
        byte[] bytes = value.toByteArray();
        int start = Math.max(0, bytes.length - 32);
        int length = bytes.length - start;
        System.arraycopy(bytes, start, out, offset + 32 - length, length);
    }

    private static ECParameterSpec parameters() {
        return ((ECPublicKey) generate().getPublic()).getParams();
    }
}
