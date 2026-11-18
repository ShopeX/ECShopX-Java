package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.config.McpPublicUrls;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthorizationServerMetadataController {

	private final McpProperties mcpProperties;

	public AuthorizationServerMetadataController(McpProperties mcpProperties) {
		this.mcpProperties = mcpProperties;
	}

	@GetMapping(
			value = {
				"/.well-known/oauth-authorization-server",
				"/.well-known/oauth-authorization-server/api/mcp",
				"/api/mcp/.well-known/oauth-authorization-server"
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> metadata() {
		String base = McpPublicUrls.publicBase(mcpProperties);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("issuer", base);
		body.put("authorization_endpoint", base + "/api/mcp/oauth/authorize");
		body.put("token_endpoint", base + "/api/mcp/oauth/token");
		body.put("registration_endpoint", base + "/api/mcp/oauth/register");
		body.put("code_challenge_methods_supported", List.of("S256"));
		body.put("response_types_supported", List.of("code"));
		return body;
	}
}