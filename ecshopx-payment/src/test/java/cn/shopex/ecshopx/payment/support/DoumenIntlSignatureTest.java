package cn.shopex.ecshopx.payment.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DoumenIntlSignatureTest {

	@Test
	void signAndVerify_caseInsensitive() {
		String body = "{\"status\":\"SUCCEED\",\"merchantOrderId\":\"T1\"}";
		String secret = "SK";
		String sig = DoumenIntlSignature.signPostBody(body, secret);
		assertTrue(DoumenIntlSignature.verifyNotify(body, secret, sig));
		assertTrue(DoumenIntlSignature.verifyNotify(body, secret, sig.toUpperCase()));
		assertFalse(DoumenIntlSignature.verifyNotify(body, secret, "deadbeef"));
		assertEquals(32, sig.length());
	}
}
