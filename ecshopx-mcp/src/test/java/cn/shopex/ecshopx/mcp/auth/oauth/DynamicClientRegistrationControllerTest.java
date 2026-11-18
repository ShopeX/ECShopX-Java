package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicClientRegistrationControllerTest {

	private MockMvc mockMvc;

	private McpProperties mcpProperties;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mcpProperties = new McpProperties();
		OAuthClientRegistry registry = new InMemoryOAuthClientRegistry();
		DcrRateLimiter rateLimiter = new DcrRateLimiter(mcpProperties);
		mockMvc = MockMvcBuilders
				.standaloneSetup(new DynamicClientRegistrationController(registry, rateLimiter, mcpProperties))
				.build();
	}

	@Test
	void registerReturnsClientIdForHttpsRedirect() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "redirect_uris": ["https://client.example/callback"],
						  "client_name": "Test MCP Client"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.client_id").isNotEmpty())
				.andExpect(jsonPath("$.redirect_uris[0]").value("https://client.example/callback"))
				.andExpect(jsonPath("$.client_id_issued_at").isNumber());
	}

	@Test
	void registerAllowsLocalhostHttpRedirect() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"redirect_uris": ["http://localhost:5173/oauth/callback"]}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.redirect_uris[0]").value("http://localhost:5173/oauth/callback"));
	}

	@Test
	void registerRejectsNonLocalHttpRedirect() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"redirect_uris": ["http://evil.example/callback"]}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registerRejectsMissingRedirectUris() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registerIgnoresUnknownOauthMetadataFieldsFromCursor() throws Exception {
		mockMvc.perform(post("/api/mcp/oauth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "redirect_uris": ["http://127.0.0.1:8787/callback"],
						  "client_name": "Cursor",
						  "token_endpoint_auth_method": "none",
						  "grant_types": ["authorization_code", "refresh_token"],
						  "response_types": ["code"],
						  "scope": "openid"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.client_id").isNotEmpty());
	}

	@Test
	void registerReturns429WhenRateLimited() throws Exception {
		mcpProperties.getDcr().setRateLimitPerMinute(2);
		String body = """
				{"redirect_uris": ["https://a.example/cb"]}
				""";
		mockMvc.perform(post("/api/mcp/oauth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/mcp/oauth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		MvcResult third = mockMvc
				.perform(post("/api/mcp/oauth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isTooManyRequests())
				.andReturn();
		JsonNode json = objectMapper.readTree(third.getResponse().getContentAsString());
		assertNotNull(json.get("error"));
		assertTrue(json.get("error").asText().contains("rate"));
	}

	@Test
	void registerRateLimitIgnoresSpoofedXForwardedForByDefault() throws Exception {
		mcpProperties.getDcr().setRateLimitPerMinute(2);
		String body = """
				{"redirect_uris": ["https://b.example/cb"]}
				""";
		mockMvc.perform(post("/api/mcp/oauth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/mcp/oauth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/mcp/oauth/register")
						.header("X-Forwarded-For", "203.0.113.99")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isTooManyRequests());
	}
}