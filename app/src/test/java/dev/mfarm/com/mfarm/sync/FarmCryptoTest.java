package dev.mfarm.com.mfarm.sync;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FarmCryptoTest {
    @Test
    public void encryptDecryptRoundTrip() throws Exception {
        String json = "{\"farmId\":\"abc\",\"records\":[]}";
        byte[] blob = FarmCrypto.encrypt(json, "K7P2-QM4N");
        String plain = FarmCrypto.decrypt(blob, "k7p2qm4n");
        assertEquals(json, plain);
    }

    @Test(expected = Exception.class)
    public void wrongCodeFails() throws Exception {
        byte[] blob = FarmCrypto.encrypt("secret", "AAAAAAAA");
        FarmCrypto.decrypt(blob, "BBBBBBBB");
    }
}
