package cn.shopex.ecshopx.mcp.tool.grant;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.tool.HttpToolDeclaration;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutionException;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutor;
import cn.shopex.ecshopx.mcp.tool.ToolAuthContext;
import cn.shopex.ecshopx.mcp.tool.ToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MemberCouponGrantToolCallback implements ToolCallback {

	public static final String TOOL_NAME = "member_coupon_grant";

	private static final String INPUT_SCHEMA = """
			{"type":"object","properties":{"userids":{"type":"array","items":{"type":"integer"},"description":"member user_id list"},"couponsids":{"type":"array","items":{"type":"integer"},"description":"discount card_id list"},"confirm":{"type":"boolean"}},"required":["userids","couponsids","confirm"],"additionalProperties":true}""";

	private static final HttpToolDeclaration GRANT_DECLARATION = new HttpToolDeclaration(
			TOOL_NAME,
			"Grant coupons to members via POST /promotions/activity/give. Requires confirm=true. "
					+ "userids = member user_id; couponsids = card_id. Recipient count capped by "
					+ "ecx.mcp.couponGrantMaxRecipients (default 50). Prefer this over admin_api_call.",
			INPUT_SCHEMA,
			"POST",
			"/api/v1/promotions/activity/give",
			true,
			Set.of());

	private final LoopbackToolExecutor executor;
	private final McpProperties properties;
	private final ObjectMapper objectMapper;
	private final ToolDefinition toolDefinition;

	public MemberCouponGrantToolCallback(LoopbackToolExecutor executor, McpProperties properties) {
		this(executor, properties, new ObjectMapper());
	}

	MemberCouponGrantToolCallback(LoopbackToolExecutor executor, McpProperties properties, ObjectMapper objectMapper) {
		this.executor = executor;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.toolDefinition = DefaultToolDefinition.builder()
				.name(TOOL_NAME)
				.description(GRANT_DECLARATION.description())
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
		requireConfirm(args);
		List<Object> userids = parseNonEmptyArray(args.get("userids"), "userids");
		List<Object> couponsids = parseNonEmptyArray(args.get("couponsids"), "couponsids");
		int max = properties.getCouponGrantMaxRecipients();
		if (userids.size() > max) {
			throw new IllegalArgumentException(
					"userids size " + userids.size() + " exceeds max " + max + " for tool: " + TOOL_NAME);
		}
		Map<String, Object> normalized = new LinkedHashMap<>();
		normalized.put("confirm", Boolean.TRUE);
		normalized.put("userids", userids);
		normalized.put("couponsids", couponsids);
		String argsJson = toJson(normalized);
		ToolResult result = executor.execute(GRANT_DECLARATION, argsJson, ToolAuthContext.getBearer());
		if (result.isError()) {
			throw new LoopbackToolExecutionException(result.text());
		}
		return result.text();
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

	private static void requireConfirm(Map<String, Object> args) {
		Object confirm = args.get("confirm");
		if (!Boolean.TRUE.equals(confirm) && !"true".equalsIgnoreCase(String.valueOf(confirm))) {
			throw new IllegalArgumentException("confirm=true is required for tool: " + TOOL_NAME);
		}
	}

	private List<Object> parseNonEmptyArray(Object raw, String fieldName) {
		List<Object> list = coerceToList(raw, fieldName);
		if (list.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must be a non-empty array for tool: " + TOOL_NAME);
		}
		return list;
	}

	private static List<Object> coerceToList(Object raw, String fieldName) {
		if (raw == null) {
			throw new IllegalArgumentException("Missing required argument: " + fieldName);
		}
		if (raw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			String[] parts = s.split(",");
			List<Object> out = new ArrayList<>();
			for (String part : parts) {
				String trimmed = part.trim();
				if (StringUtils.hasText(trimmed)) {
					out.add(parseScalar(trimmed));
				}
			}
			return out;
		}
		throw new IllegalArgumentException(fieldName + " must be a JSON array or comma-separated string");
	}

	private static Object parseScalar(String trimmed) {
		try {
			if (trimmed.matches("-?\\d+")) {
				return Long.parseLong(trimmed);
			}
		}
		catch (NumberFormatException ignored) {
			// fall through as string
		}
		return trimmed;
	}

	private String toJson(Map<String, Object> map) {
		try {
			return objectMapper.writeValueAsString(map);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize tool arguments", ex);
		}
	}
}