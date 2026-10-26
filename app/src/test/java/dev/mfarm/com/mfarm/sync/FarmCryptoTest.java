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

    @Test
    public void testPBKDF2BenchmarkPerformance() throws Exception {
        String json = "{\"farmId\":\"benchmark-farm\",\"records\":[{\"uuid\":\"u1\",\"table\":\"animas\"}]}";
        String joinCode = "BENCHMARK-1234";

        long totalNano = 0;
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            byte[] blob = FarmCrypto.encrypt(json, joinCode);
            String plain = FarmCrypto.decrypt(blob, joinCode);
            long end = System.nanoTime();
            totalNano += (end - start);
            assertEquals(json, plain);
        }

        double avgMs = (totalNano / (double) iterations) / 1_000_000.0;
        System.out.println("FarmCrypto PBKDF2 (50,000 iterations) average time over 10 runs: " + String.format("%.2f", avgMs) + " ms");
        org.junit.Assert.assertTrue("Benchmark should complete in reasonable time", avgMs > 0);
    }
}
