package cn.shopex.ecshopx.common.crypto;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SensitiveFieldEncryptorAlwaysTest {

	private static final String APP_KEY = "base64:MTIzNDU2Nzg5MDEyMzQ1Ng==";

	@Test
	void encryptAlwaysIgnoresSwitchOffAndRoundTrips() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		String cipher = enc.encryptAlways("Abcd1234");
		assertNotEquals("Abcd1234", cipher);
		assertEquals("Abcd1234", enc.decryptAlways(cipher));
		assertEquals("Abcd1234", enc.encrypt("Abcd1234"));
	}

	@Test
	void encryptAlwaysFailsWhenKeyMissing() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, "", "AES-128-CBC");
		assertThrows(IllegalStateException.class, () -> enc.encryptAlways("Abcd1234"));
	}

	@Test
	void encryptAlwaysRejectsBlank() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		assertThrows(IllegalArgumentException.class, () -> enc.encryptAlways(""));
	}

	@Test
	void decryptAlwaysFailsOnGarbage() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		IllegalStateException ex =
				assertThrows(IllegalStateException.class, () -> enc.decryptAlways("not-cipher"));
		assertTrue(ex.getMessage() != null && !ex.getMessage().isBlank());
	}
}
