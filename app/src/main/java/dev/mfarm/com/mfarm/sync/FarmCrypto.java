package dev.mfarm.com.mfarm.sync;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Backup files are encrypted with the farm join code so a Drive folder or
 * WhatsApp forward is not readable without the code.
 */
public final class FarmCrypto {
    private static final byte[] MAGIC = new byte[]{'M', 'F', 'A', 'R', 'M', '1'};
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 16;
    private static final int ITERATIONS = 10_000;
    private static final int KEY_BITS = 128;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final SecureRandom RANDOM = new SecureRandom();

    private FarmCrypto() {}

    public static byte[] encrypt(String plaintext, String joinCode) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);
        SecretKey key = deriveKey(joinCode, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(UTF8));
        byte[] out = new byte[MAGIC.length + SALT_LEN + IV_LEN + cipherText.length];
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
        System.arraycopy(salt, 0, out, MAGIC.length, SALT_LEN);
        System.arraycopy(iv, 0, out, MAGIC.length + SALT_LEN, IV_LEN);
        System.arraycopy(cipherText, 0, out, MAGIC.length + SALT_LEN + IV_LEN, cipherText.length);
        return out;
    }

    public static String decrypt(byte[] blob, String joinCode) throws GeneralSecurityException {
        if (blob == null || blob.length < MAGIC.length + SALT_LEN + IV_LEN + 16) {
            throw new GeneralSecurityException("Backup file is too small");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (blob[i] != MAGIC[i]) {
                throw new GeneralSecurityException("Not an MFarm backup");
            }
        }
        byte[] salt = Arrays.copyOfRange(blob, MAGIC.length, MAGIC.length + SALT_LEN);
        byte[] iv = Arrays.copyOfRange(blob, MAGIC.length + SALT_LEN, MAGIC.length + SALT_LEN + IV_LEN);
        byte[] cipherText = Arrays.copyOfRange(blob, MAGIC.length + SALT_LEN + IV_LEN, blob.length);
        SecretKey key = deriveKey(joinCode, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, UTF8);
    }

    private static SecretKey deriveKey(String joinCode, byte[] salt) throws GeneralSecurityException {
        String password = FarmIdentity.normalizeJoinCode(joinCode);
        if (password.isEmpty()) {
            throw new GeneralSecurityException("Join code required");
        }
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        byte[] encoded = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }
}
