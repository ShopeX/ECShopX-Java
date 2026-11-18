package cn.shopex.ecshopx.mcp.tool.smoke;

import cn.shopex.ecshopx.mcp.config.McpProperties;
import cn.shopex.ecshopx.mcp.tool.HttpToolDeclaration;

public final class McpSmokeTools {

	public static final String TOOL_NAME = "mcp_smoke_get";

	private static final String INPUT_SCHEMA = """
			{"type":"object","properties":{},"additionalProperties":false}""";

	private McpSmokeTools() {
	}

	public static HttpToolDeclaration smokeDeclaration(McpProperties properties) {
		return HttpToolDeclaration.read(
				TOOL_NAME,
				"Smoke loopback GET to verify MCP bearer forwarding",
				INPUT_SCHEMA,
				"GET",
				properties.getSmokePath());
	}
}