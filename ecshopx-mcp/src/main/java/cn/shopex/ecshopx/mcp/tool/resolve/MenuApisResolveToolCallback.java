package cn.shopex.ecshopx.mcp.tool.resolve;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalog;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiEntry;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MenuApisResolveToolCallback implements ToolCallback {

	public static final String TOOL_NAME = "menu_apis_resolve";

	private static final String INPUT_SCHEMA = """
			{"type":"object","properties":{"query":{"type":"string"},"route":{"type":"string"},"permission":{"type":"string"}},"additionalProperties":true}""";

	private final MenuApiCatalog catalog;
	private final ApiLoopbackClient loopbackClient;
	private final ObjectMapper objectMapper;
	private final ToolDefinition toolDefinition;

	public MenuApisResolveToolCallback(MenuApiCatalog catalog, ApiLoopbackClient loopbackClient) {
		this(catalog, loopbackClient, new ObjectMapper());
	}

	public MenuApisResolveToolCallback(
			MenuApiCatalog catalog, ApiLoopbackClient loopbackClient, ObjectMapper objectMapper) {
		this.catalog = catalog;
		this.loopbackClient = loopbackClient;
		this.objectMapper = objectMapper;
		this.toolDefinition = DefaultToolDefinition.builder()
				.name(TOOL_NAME)
				.description("""
						Resolve menu-api-catalog entries visible under current operator menus. \
						Provide at least one of: permission/route (English menu keys or catalog path \
						template like /api/v1/orders), or query (substring over catalog path/routes/permissions — \
						Chinese menu titles usually miss). Empty [] means no intersection, not a transport error. \
						Prefer fixed tools when available; else use returned method/path (templates) with admin_api_call.""")
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
		String query = stringArg(args.get("query"));
		String route = stringArg(args.get("route"));
		String permission = stringArg(args.get("permission"));
		if (!StringUtils.hasText(query) && !StringUtils.hasText(route) && !StringUtils.hasText(permission)) {
			throw new IllegalArgumentException(
					"At least one of query, route, or permission is required for tool: " + TOOL_NAME);
		}

		Set<String> menuKeys = MenusPermissionSupport.fetchMenuKeys(loopbackClient, null, objectMapper);
		Set<MenuApiEntry> hits = new LinkedHashSet<>();

		if (StringUtils.hasText(query)) {
			catalog.resolve(query, menuKeys).forEach(hits::add);
		}
		if (StringUtils.hasText(route)) {
			catalog.findByRouteOrPermission(route).stream()
					.filter(e -> e.intersectsMenuKeys(menuKeys))
					.forEach(hits::add);
		}
		if (StringUtils.hasText(permission)) {
			catalog.findByRouteOrPermission(permission).stream()
					.filter(e -> e.intersectsMenuKeys(menuKeys))
					.forEach(hits::add);
		}

		List<Map<String, Object>> payload = new ArrayList<>();
		for (MenuApiEntry entry : hits) {
			payload.add(toMap(entry));
		}
		return writeJson(payload);
	}

	private static Map<String, Object> toMap(MenuApiEntry entry) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("routes", entry.routes());
		map.put("permissions", entry.permissions());
		map.put("method", entry.method());
		map.put("path", entry.path());
		map.put("risk", entry.risk());
		map.put("params", entry.paramHints());
		return map;
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

	private static String stringArg(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw);
		return StringUtils.hasText(s) ? s.trim() : null;
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize resolve result", ex);
		}
	}
}