package cn.shopex.ecshopx.mcp.tool.resolve;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.loopback.LoopbackRequest;
import cn.shopex.ecshopx.mcp.loopback.LoopbackResponse;
import cn.shopex.ecshopx.mcp.tool.ToolAuthContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MenusPermissionSupport {

	private static final String PERMISSION_PATH = "/api/v1/permission";

	private static final Set<String> COLLECT_FIELD_NAMES = Set.of(
			"url", "path", "alias", "aliasname", "permission", "name", "auth");

	private MenusPermissionSupport() {
	}

	public static Set<String> fetchMenuKeys(ApiLoopbackClient loopbackClient, String bearer, ObjectMapper objectMapper) {
		String effectiveBearer = resolveBearer(bearer);
		Map<String, String> headers = buildHeaders(effectiveBearer);
		LoopbackResponse response = loopbackClient.execute(
				new LoopbackRequest("GET", PERMISSION_PATH, Map.of(), headers, null));
		if (response.status() < 200 || response.status() >= 300) {
			throw new IllegalStateException(
					"Failed to load menus from " + PERMISSION_PATH + ": HTTP " + response.status());
		}
		String body = response.body() != null ? response.body() : "";
		if (!StringUtils.hasText(body)) {
			return Set.of();
		}
		try {
			JsonNode root = objectMapper.readTree(body);
			Set<String> keys = new LinkedHashSet<>();
			collectMenuKeys(root, keys);
			return Set.copyOf(keys);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to parse permission response", ex);
		}
	}

	private static void collectMenuKeys(JsonNode node, Set<String> keys) {
		if (node == null || node.isNull()) {
			return;
		}
		if (node.isObject()) {
			node.fields().forEachRemaining(field -> {
				String name = field.getKey();
				JsonNode value = field.getValue();
				if (COLLECT_FIELD_NAMES.contains(name.toLowerCase(Locale.ROOT)) && value != null && value.isTextual()) {
					String text = value.asText();
					if (StringUtils.hasText(text)) {
						keys.add(text);
					}
				}
				collectMenuKeys(value, keys);
			});
			return;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				collectMenuKeys(child, keys);
			}
		}
	}

	private static String resolveBearer(String bearerToken) {
		if (StringUtils.hasText(bearerToken)) {
			return bearerToken;
		}
		return ToolAuthContext.getBearer();
	}

	private static Map<String, String> buildHeaders(String bearerToken) {
		if (!StringUtils.hasText(bearerToken)) {
			return Map.of();
		}
		String authorization = bearerToken.strip();
		if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
			authorization = "Bearer " + authorization;
		}
		return Map.of(HttpHeaders.AUTHORIZATION, authorization);
	}
}