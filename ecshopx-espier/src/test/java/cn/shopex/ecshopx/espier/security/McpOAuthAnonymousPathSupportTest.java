package cn.shopex.ecshopx.espier.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class McpOAuthAnonymousPathSupportTest {

	@Test
	void matchesMcpAndOAuthPaths() {
		assertTrue(McpOAuthAnonymousPathSupport.matches("/api/mcp"));
		assertTrue(McpOAuthAnonymousPathSupport.matches("/api/mcp/oauth/token"));
		assertTrue(McpOAuthAnonymousPathSupport.matches("/api/mcp/oauth/authorize/login"));
	}

	@Test
	void matchesWellKnownPaths() {
		assertTrue(McpOAuthAnonymousPathSupport.matches("/.well-known/oauth-protected-resource"));
		assertTrue(McpOAuthAnonymousPathSupport.matches("/.well-known/oauth-protected-resource/mcp"));
		assertTrue(McpOAuthAnonymousPathSupport.matches("/.well-known/oauth-protected-resource/api/mcp"));
		assertTrue(McpOAuthAnonymousPathSupport.matches("/.well-known/oauth-authorization-server"));
		assertTrue(McpOAuthAnonymousPathSupport.matches("/.well-known/oauth-authorization-server/api/mcp"));
	}

	@Test
	void doesNotMatchUnrelatedApiPaths() {
		assertFalse(McpOAuthAnonymousPathSupport.matches("/api/v1/operator/login"));
		assertFalse(McpOAuthAnonymousPathSupport.matches("/api/v1/pos/auth/login"));
		assertFalse(McpOAuthAnonymousPathSupport.matches("/api/mcp-evil"));
		assertFalse(McpOAuthAnonymousPathSupport.matches(null));
	}
}