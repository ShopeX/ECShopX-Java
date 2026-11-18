package cn.shopex.ecshopx.mcp.auth;

import cn.shopex.ecshopx.mcp.auth.oauth.AuthorizationServerMetadataController;
import cn.shopex.ecshopx.mcp.auth.oauth.ProtectedResourceMetadataController;
import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.tool.ToolAuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpOAuthDiscoveryTest {

	private McpProperties mcpProperties;

	private MockMvc discoveryMvc;

	@BeforeEach
	void setUp() {
		mcpProperties = new McpProperties();
		mcpProperties.setPublicBaseUrl("https://mcp.example.com");
		mcpProperties.setMcpPath("/api/mcp");
		discoveryMvc = MockMvcBuilders.standaloneSetup(
						new ProtectedResourceMetadataController(mcpProperties),
						new AuthorizationServerMetadataController(mcpProperties))
				.build();
	}

	@AfterEach
	void tearDown() {
		ToolAuthContext.clear();
	}

	@Test
	void protectedResourceMetadataAtWellKnownRoot() throws Exception {
		discoveryMvc.perform(get("/.well-known/oauth-protected-resource"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resource").value("https://mcp.example.com/api/mcp"))
				.andExpect(jsonPath("$.authorization_servers[0]").value("https://mcp.example.com"))
				.andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"));
	}

	@Test
	void protectedResourceMetadataAtMcpSuffix() throws Exception {
		discoveryMvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resource").value("https://mcp.example.com/api/mcp"));
	}

	@Test
	void protectedResourceMetadataAtApiMcpPathSuffix() throws Exception {
		discoveryMvc.perform(get("/.well-known/oauth-protected-resource/api/mcp"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resource").value("https://mcp.example.com/api/mcp"));
	}

	@Test
	void authorizationServerMetadata() throws Exception {
		discoveryMvc.perform(get("/.well-known/oauth-authorization-server"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issuer").value("https://mcp.example.com"))
				.andExpect(jsonPath("$.authorization_endpoint")
						.value("https://mcp.example.com/api/mcp/oauth/authorize"))
				.andExpect(jsonPath("$.token_endpoint").value("https://mcp.example.com/api/mcp/oauth/token"))
				.andExpect(jsonPath("$.registration_endpoint")
						.value("https://mcp.example.com/api/mcp/oauth/register"))
				.andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"))
				.andExpect(jsonPath("$.response_types_supported[0]").value("code"));
	}

	@Test
	void authorizationServerMetadataAtApiMcpPathSuffix() throws Exception {
		discoveryMvc.perform(get("/.well-known/oauth-authorization-server/api/mcp"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issuer").value("https://mcp.example.com"));
	}

	@Test
	void mcpBearerAuthFilterReturns401WithoutAuthorization() throws Exception {
		McpBearerAuthFilter filter = new McpBearerAuthFilter(mcpProperties);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/mcp");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		assertEquals(401, response.getStatus());
		assertEquals(
				"Bearer realm=\"mcp\", resource_metadata=\"https://mcp.example.com/.well-known/oauth-protected-resource/api/mcp\"",
				response.getHeader("WWW-Authenticate"));
	}

	@Test
	void mcpBearerAuthFilterSetsToolAuthContextForBearer() throws Exception {
		McpBearerAuthFilter filter = new McpBearerAuthFilter(mcpProperties);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/mcp");
		request.addHeader("Authorization", "Bearer business-jwt");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> duringChain = new AtomicReference<>();
		filter.doFilter(request, response, (req, res) -> duringChain.set(ToolAuthContext.getBearer()));
		assertEquals("Bearer business-jwt", duringChain.get());
		assertNull(ToolAuthContext.getBearer());
		assertEquals(200, response.getStatus());
	}

	@Test
	void mcpBearerAuthFilterSkipsOtherPaths() throws Exception {
		McpBearerAuthFilter filter = new McpBearerAuthFilter(mcpProperties);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mcp/oauth/register");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		assertEquals(200, response.getStatus());
		assertNull(response.getHeader("WWW-Authenticate"));
	}

	@Test
	void publicBaseFallsBackToApiBaseUrl() throws Exception {
		mcpProperties.setPublicBaseUrl("");
		mcpProperties.setApiBaseUrl("http://127.0.0.1:8080");
		discoveryMvc = MockMvcBuilders.standaloneSetup(new ProtectedResourceMetadataController(mcpProperties))
				.build();
		discoveryMvc.perform(get("/.well-known/oauth-protected-resource"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resource").value("http://127.0.0.1:8080/api/mcp"));
	}
}