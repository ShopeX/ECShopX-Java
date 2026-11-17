package cn.shopex.ecshopx.mcp.config;

import org.springframework.util.StringUtils;

public final class McpPublicUrls {

	private McpPublicUrls() {
	}

	public static String publicBase(McpProperties properties) {
		String configured = properties.getPublicBaseUrl();
		String base = StringUtils.hasText(configured) ? configured.trim() : properties.getApiBaseUrl().trim();
		return stripTrailingSlash(base);
	}

	public static String resourceMetadataUrl(McpProperties properties) {
		return publicBase(properties) + "/.well-known/oauth-protected-resource";
	}

	public static String mcpResourceUrl(McpProperties properties) {
		String path = properties.getMcpPath();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return publicBase(properties) + path;
	}

	private static String stripTrailingSlash(String value) {
		if (value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}
}