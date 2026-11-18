package cn.shopex.ecshopx.mcp.tool;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.loopback.LoopbackRequest;
import cn.shopex.ecshopx.mcp.loopback.LoopbackResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoopbackToolExecutor {

	private static final Logger log = LoggerFactory.getLogger(LoopbackToolExecutor.class);

	private static final Pattern PATH_PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

	private static final Set<String> RESERVED_ARG_KEYS = Set.of("confirm", "force");

	private final ApiLoopbackClient loopbackClient;
	private final ObjectMapper objectMapper;

	public LoopbackToolExecutor(ApiLoopbackClient loopbackClient) {
		this(loopbackClient, new ObjectMapper());
	}

	public LoopbackToolExecutor(ApiLoopbackClient loopbackClient, ObjectMapper objectMapper) {
		this.loopbackClient = loopbackClient;
		this.objectMapper = objectMapper;
	}

	public ToolResult execute(HttpToolDeclaration declaration, String argsJson, String bearerToken) {
		String effectiveBearer = resolveBearer(bearerToken);
		Map<String, Object> args = parseArgs(argsJson);
		validateGuards(declaration, args);
		Set<String> pathKeys = pathPlaceholderKeys(declaration.pathTemplate());
		String path = resolvePath(declaration.pathTemplate(), args, pathKeys);
		Map<String, ?> query = buildQuery(declaration.method(), args, pathKeys);
		String body = buildBody(declaration.method(), args, pathKeys);
		Map<String, String> headers = buildHeaders(effectiveBearer);

		long startNanos = System.nanoTime();
		try {
			LoopbackResponse response = loopbackClient.execute(
					new LoopbackRequest(declaration.method(), path, query, headers, body));
			long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
			log.info(
					"tool={} http.method={} http.path={} http.status={} latency_ms={} authorization_present={}",
					declaration.name(),
					declaration.method(),
					path,
					response.status(),
					latencyMs,
					StringUtils.hasText(effectiveBearer));
			return mapHttpResponse(response);
		}
		catch (ResourceAccessException ex) {
			long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
			log.warn(
					"tool={} http.method={} http.path={} loopback_error=timeout latency_ms={}",
					declaration.name(),
					declaration.method(),
					path,
					latencyMs);
			return new ToolResult(true, "HTTP status timeout: loopback request failed (" + ex.getMessage() + ")");
		}
	}

	private static String resolveBearer(String bearerToken) {
		if (StringUtils.hasText(bearerToken)) {
			return bearerToken;
		}
		return ToolAuthContext.getBearer();
	}

	private static void validateGuards(HttpToolDeclaration declaration, Map<String, Object> args) {
		for (String key : declaration.requiredArgs()) {
			Object v = args.get(key);
			if (v == null || (v instanceof String s && !StringUtils.hasText(s))) {
				throw new IllegalArgumentException("Missing required argument: " + key);
			}
		}
		if (declaration.requireConfirm() && !isTruthyTrue(args.get("confirm"))) {
			throw new IllegalArgumentException("confirm=true is required for tool: " + declaration.name());
		}
	}

	private static boolean isTruthyTrue(Object confirm) {
		return Boolean.TRUE.equals(confirm) || "true".equalsIgnoreCase(String.valueOf(confirm));
	}

	private Map<String, Object> parseArgs(String argsJson) {
		if (!StringUtils.hasText(argsJson) || "{}".equals(argsJson.trim())) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(argsJson, new TypeReference<>() {
			});
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid tool arguments JSON", ex);
		}
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

	private static Map<String, ?> buildQuery(String method, Map<String, Object> args, Set<String> pathKeys) {
		if (methodUsesBody(method)) {
			return Map.of();
		}
		Map<String, Object> query = new LinkedHashMap<>();
		args.forEach((key, value) -> {
			if (!pathKeys.contains(key) && !RESERVED_ARG_KEYS.contains(key) && value != null) {
				query.put(key, value);
			}
		});
		return query.isEmpty() ? Map.of() : query;
	}

	private String buildBody(String method, Map<String, Object> args, Set<String> pathKeys) {
		if (!methodUsesBody(method)) {
			return null;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		args.forEach((key, value) -> {
			if (!pathKeys.contains(key) && !RESERVED_ARG_KEYS.contains(key) && value != null) {
				body.put(key, value);
			}
		});
		if (body.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(body);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize tool body", ex);
		}
	}

	private static boolean methodUsesBody(String method) {
		HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
		return httpMethod == HttpMethod.POST
				|| httpMethod == HttpMethod.PUT
				|| httpMethod == HttpMethod.PATCH;
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

	private static ToolResult mapHttpResponse(LoopbackResponse response) {
		int status = response.status();
		String body = response.body() != null ? response.body() : "";
		if (status >= 200 && status < 300) {
			return new ToolResult(false, body);
		}
		String text = "HTTP status " + status;
		if (StringUtils.hasText(body)) {
			text = text + "\n" + body;
		}
		return new ToolResult(true, text);
	}
}