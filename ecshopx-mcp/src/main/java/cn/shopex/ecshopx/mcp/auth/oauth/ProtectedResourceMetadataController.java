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
public class ProtectedResourceMetadataController {

	private final McpProperties mcpProperties;

	public ProtectedResourceMetadataController(McpProperties mcpProperties) {
		this.mcpProperties = mcpProperties;
	}

	@GetMapping(
			value = {
				"/.well-known/oauth-protected-resource",
				"/.well-known/oauth-protected-resource/mcp",
				// Cursor / RFC 9728 path-appended form when resource is .../api/mcp
				"/.well-known/oauth-protected-resource/api/mcp",
				"/api/mcp/.well-known/oauth-protected-resource"
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> metadata() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("resource", McpPublicUrls.mcpResourceUrl(mcpProperties));
		body.put("authorization_servers", List.of(McpPublicUrls.publicBase(mcpProperties)));
		body.put("bearer_methods_supported", List.of("header"));
		return body;
	}
}