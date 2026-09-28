package dev.mfarm.com.mfarm.sync;

import org.junit.Test;

import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FarmCryptoTest {

    @Test
    public void encryptDecryptV2RoundTrip() throws Exception {
        String json = "{\"farmId\":\"abc-123\",\"records\":[{\"uuid\":\"u1\",\"table\":\"animas\"}]}";
        byte[] blob = FarmCrypto.encrypt(json, "K7P2-QM4N");
        String plain = FarmCrypto.decrypt(blob, "k7p2qm4n");
        assertEquals(json, plain);
    }

    @Test
    public void wrongCodeFailsDecrypt() throws Exception {
        byte[] blob = FarmCrypto.encrypt("secret-data", "CODE1234");
        try {
            FarmCrypto.decrypt(blob, "WRONGCODE");
            fail("Decrypting with wrong code must throw GeneralSecurityException");
        } catch (GeneralSecurityException e) {
            // Expected
        }
    }

    @Test
    public void tamperedCiphertextFailsDecrypt() throws Exception {
        byte[] blob = FarmCrypto.encrypt("secret-data", "CODE1234");
        // Tamper cipherText byte at the end
        blob[blob.length - 1] ^= 0xFF;
        try {
            FarmCrypto.decrypt(blob, "CODE1234");
            fail("Decrypting tampered ciphertext must throw GeneralSecurityException");
        } catch (GeneralSecurityException e) {
            // Expected
        }
    }

    @Test
    public void tamperedMacTagFailsDecrypt() throws Exception {
        byte[] blob = FarmCrypto.encrypt("secret-data", "CODE1234");
        // Tamper HMAC tag byte (offset 38)
        blob[38] ^= 0xFF;
        try {
            FarmCrypto.decrypt(blob, "CODE1234");
            fail("Decrypting with tampered MAC tag must throw GeneralSecurityException");
        } catch (GeneralSecurityException e) {
            // Expected
        }
    }

    @Test
    public void truncatedBlobFailsDecrypt() {
        byte[] tiny = new byte[]{1, 2, 3};
        try {
            FarmCrypto.decrypt(tiny, "CODE1234");
            fail("Truncated blob must fail decrypt");
        } catch (Exception e) {
            // Expected
        }
    }
}
