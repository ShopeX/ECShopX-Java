package cn.shopex.ecshopx.mcp.tool.resolve;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.loopback.LoopbackRequest;
import cn.shopex.ecshopx.mcp.loopback.LoopbackResponse;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutionException;
import cn.shopex.ecshopx.mcp.tool.ToolAuthContext;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalog;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AdminApiCallToolCallback implements ToolCallback {

	public static final String TOOL_NAME = "admin_api_call";

	private static final String API_V1_PREFIX = "/api/v1/";

	private static final Set<String> RESERVED = Set.of("method", "path", "query", "body", "confirm", "force");

	private static final Pattern PATH_PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

	private static final String INPUT_SCHEMA = """
			{"type":"object","properties":{"method":{"type":"string"},"path":{"type":"string"},"query":{"type":"object"},"body":{"type":"object"},"confirm":{"type":"boolean"},"force":{"type":"boolean"}},"required":["method","path"],"additionalProperties":true}""";

	private final MenuApiCatalog catalog;
	private final ApiLoopbackClient loopbackClient;
	private final ObjectMapper objectMapper;
	private final ToolDefinition toolDefinition;

	public AdminApiCallToolCallback(MenuApiCatalog catalog, ApiLoopbackClient loopbackClient) {
		this(catalog, loopbackClient, new ObjectMapper());
	}

	public AdminApiCallToolCallback(MenuApiCatalog catalog, ApiLoopbackClient loopbackClient, ObjectMapper objectMapper) {
		this.catalog = catalog;
		this.loopbackClient = loopbackClient;
		this.objectMapper = objectMapper;
		this.toolDefinition = DefaultToolDefinition.builder()
				.name(TOOL_NAME)
				.description("""
						Call a catalog-registered admin API via loopback. path must be the catalog template \
						(e.g. /api/v1/order/{order_id}) starting with /api/v1/, plus path args as top-level fields. \
						Prefer fixed tools when they exist. write-risk needs confirm=true; destructive is blocked. \
						force=true only skips menu∩catalog check — still requires catalog hit and confirm for writes. \
						Prefer member_coupon_grant for bulk give.""")
				.inputSchema(INPUT_SCHEMA)
				.build();
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return toolDefinition;
	}

	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		Map<String, Object> args = parseArgs(toolInput);
		String method = requiredString(args.get("method"), "method");
		String path = requiredString(args.get("path"), "path");
		boolean force = isTruthyTrue(args.get("force"));
		boolean confirm = isTruthyTrue(args.get("confirm"));

		validatePathPrefix(path);
		List<MenuApiEntry> entries = catalog.findByMethodAndPath(method, path);
		if (entries.isEmpty()) {
			throw new IllegalArgumentException(
					"No catalog entry for " + method.toUpperCase(Locale.ROOT) + " " + path + " in tool: " + TOOL_NAME);
		}
		validateRisk(entries, confirm);
		if (!force) {
			Set<String> menuKeys = MenusPermissionSupport.fetchMenuKeys(loopbackClient, null, objectMapper);
			boolean allowed = entries.stream().anyMatch(e -> e.intersectsMenuKeys(menuKeys));
			if (!allowed) {
				throw new IllegalArgumentException(
						"Menu permission does not cover " + method.toUpperCase(Locale.ROOT) + " " + path
								+ " (use force=true to override) for tool: " + TOOL_NAME);
			}
		}

		Set<String> pathKeys = pathPlaceholderKeys(path);
		String resolvedPath = resolvePath(path, args, pathKeys);
		Map<String, ?> query = buildQuery(method, args, pathKeys);
		String body = buildBody(method, args, pathKeys);
		Map<String, String> headers = buildHeaders(ToolAuthContext.getBearer());

		LoopbackResponse response = loopbackClient.execute(
				new LoopbackRequest(method.toUpperCase(Locale.ROOT), resolvedPath, query, headers, body));
		int status = response.status();
		String responseBody = response.body() != null ? response.body() : "";
		if (status >= 200 && status < 300) {
			return responseBody;
		}
		String text = "HTTP status " + status;
		if (StringUtils.hasText(responseBody)) {
			text = text + "\n" + responseBody;
		}
		throw new LoopbackToolExecutionException(text);
	}

	private static void validatePathPrefix(String path) {
		if (!StringUtils.hasText(path) || !path.startsWith(API_V1_PREFIX)) {
			throw new IllegalArgumentException("path must start with " + API_V1_PREFIX + " for tool: " + TOOL_NAME);
		}
	}

	private static void validateRisk(List<MenuApiEntry> entries, boolean confirm) {
		for (MenuApiEntry entry : entries) {
			String risk = entry.risk() != null ? entry.risk().toLowerCase(Locale.ROOT) : "";
			if ("destructive".equals(risk)) {
				throw new IllegalArgumentException("destructive catalog APIs are not allowed for tool: " + TOOL_NAME);
			}
		}
		boolean requiresConfirm = entries.stream()
				.map(MenuApiEntry::risk)
				.filter(StringUtils::hasText)
				.anyMatch(r -> "write".equalsIgnoreCase(r));
		if (requiresConfirm && !confirm) {
			throw new IllegalArgumentException("confirm=true is required for write-risk tool: " + TOOL_NAME);
		}
	}

	private Map<String, ?> buildQuery(String method, Map<String, Object> args, Set<String> pathKeys) {
		if (methodUsesBody(method)) {
			return Map.of();
		}
		Map<String, Object> query = new LinkedHashMap<>();
		Object explicit = args.get("query");
		if (explicit instanceof Map<?, ?> map) {
			map.forEach((k, v) -> {
				if (k != null && v != null && !RESERVED.contains(String.valueOf(k))) {
					query.put(String.valueOf(k), v);
				}
			});
		}
		args.forEach((key, value) -> {
			if (!pathKeys.contains(key) && !RESERVED.contains(key) && value != null) {
				query.putIfAbsent(key, value);
			}
		});
		return query.isEmpty() ? Map.of() : query;
	}

	private String buildBody(String method, Map<String, Object> args, Set<String> pathKeys) {
		if (!methodUsesBody(method)) {
			return null;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		Object explicit = args.get("body");
		if (explicit instanceof Map<?, ?> map) {
			map.forEach((k, v) -> {
				if (k != null && v != null && !RESERVED.contains(String.valueOf(k))) {
					body.put(String.valueOf(k), v);
				}
			});
		}
		else if (explicit instanceof String s && StringUtils.hasText(s)) {
			return s;
		}
		else if (explicit instanceof JsonNode node && !node.isNull()) {
			try {
				return objectMapper.writeValueAsString(node);
			}
			catch (JsonProcessingException ex) {
				throw new IllegalArgumentException("Invalid body JSON", ex);
			}
		}
		args.forEach((key, value) -> {
			if (!pathKeys.contains(key) && !RESERVED.contains(key) && value != null) {
				body.putIfAbsent(key, value);
			}
		});
		if (body.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(body);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize request body", ex);
		}
	}

	private static boolean methodUsesBody(String method) {
		HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
		return httpMethod == HttpMethod.POST
				|| httpMethod == HttpMethod.PUT
				|| httpMethod == HttpMethod.PATCH;
	}

	private static Set<String> pathPlaceholderKeys(String pathTemplate) {
		Set<String> keys = new LinkedHashSet<>();
		Matcher matcher = PATH_PLACEHOLDER.matcher(pathTemplate);
		while (matcher.find()) {
			keys.add(matcher.group(1));
		}
		return keys;
	}

	private static String resolvePath(String pathTemplate, Map<String, Object> args, Set<String> pathKeys) {
		String resolved = pathTemplate;
		for (String key : pathKeys) {
			Object value = args.get(key);
			if (value == null) {
				throw new IllegalArgumentException("Missing path argument: " + key);
			}
			resolved = resolved.replace("{" + key + "}", String.valueOf(value));
		}
		return resolved;
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

	private static boolean isTruthyTrue(Object value) {
		return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
	}

	private Map<String, Object> parseArgs(String toolInput) {
		if (!StringUtils.hasText(toolInput) || "{}".equals(toolInput.trim())) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(toolInput.trim(), new TypeReference<>() {
			});
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid tool arguments JSON", ex);
		}
	}

	private static String requiredString(Object raw, String field) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw))) {
			throw new IllegalArgumentException("Missing required argument: " + field);
		}
		return String.valueOf(raw).trim();
	}
}