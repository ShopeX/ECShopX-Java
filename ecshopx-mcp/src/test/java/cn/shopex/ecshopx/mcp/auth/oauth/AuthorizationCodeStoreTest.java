package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.auth.TokenKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationCodeStoreTest {

	@Test
	void findRemovesExpiredCode() throws Exception {
		AuthorizationCodeStore store = new AuthorizationCodeStore(1L);
		String code = store.issue("c", "http://localhost/cb", "ch", "S256", TokenKind.OPERATOR, "jwt");
		assertTrue(store.find(code).isPresent());
		Thread.sleep(1100L);
		assertTrue(store.find(code).isEmpty());
	}

	@Test
	void consumeIsSingleUse() {
		AuthorizationCodeStore store = new AuthorizationCodeStore();
		String code = store.issue("c", "http://localhost/cb", "ch", "S256", TokenKind.MEMBER, "jwt");
		assertTrue(store.consume(code).isPresent());
		assertTrue(store.consume(code).isEmpty());
	}
}