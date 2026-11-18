package cn.shopex.ecshopx.mcp.config;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.AdminFixedTools;
import cn.shopex.ecshopx.mcp.tool.HttpToolDeclaration;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutor;
import cn.shopex.ecshopx.mcp.tool.catalog.MenuApiCatalogLoader;
import cn.shopex.ecshopx.mcp.tool.grant.MemberCouponGrantToolCallback;
import cn.shopex.ecshopx.mcp.tool.resolve.AdminApiCallToolCallback;
import cn.shopex.ecshopx.mcp.tool.resolve.MenuApisResolveToolCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerConfigurationTest {

	@Test
	void loopbackProviderRegistersFixedGrantResolveAndCall() {
		McpProperties properties = new McpProperties();
		ApiLoopbackClient loopbackClient = new ApiLoopbackClient(properties, RestClient.builder());
		LoopbackToolExecutor executor = new LoopbackToolExecutor(loopbackClient, new ObjectMapper());

		List<ToolCallback> callbacks = McpServerConfiguration.buildLoopbackToolCallbacks(
				executor, loopbackClient, MenuApiCatalogLoader.loadClasspath(), properties);
		Set<String> names = callbacks.stream()
				.map(ToolCallback::getToolDefinition)
				.map(def -> def.name())
				.collect(Collectors.toSet());

		assertFalse(names.contains("mcp_smoke_get"));
		assertTrue(names.contains(MemberCouponGrantToolCallback.TOOL_NAME));
		assertTrue(names.contains(MenuApisResolveToolCallback.TOOL_NAME));
		assertTrue(names.contains(AdminApiCallToolCallback.TOOL_NAME));
		AdminFixedTools.allDeclarations().stream()
				.map(HttpToolDeclaration::name)
				.forEach(expected -> assertTrue(names.contains(expected), "missing fixed tool: " + expected));

		assertFalse(names.contains("member_tag_or_remark_update"));
		assertFalse(names.contains("distributor_basic_update"));

		int fixedCount = AdminFixedTools.allDeclarations().size();
		assertEquals(fixedCount + 3, names.size());
		assertEquals(fixedCount + 3, callbacks.size());
	}
}
