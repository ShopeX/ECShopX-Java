package cn.shopex.ecshopx.companys.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CompanysActivationCipherTest {

	@Test
	void roundTripMatchesSessionToken() {
		String tok = "7e23b4cecd91a0a8500c2fb65341193c";
		String enc = CompanysActivationCipher.encryptUserPayload(1L, tok, 1700000000L);
		Map<String, String> dec = CompanysActivationCipher.decryptUserPayload(enc);
		assertEquals(tok, dec.get("token"));
	}
}
