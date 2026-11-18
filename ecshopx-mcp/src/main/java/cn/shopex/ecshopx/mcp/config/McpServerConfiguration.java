package cn.shopex.ecshopx.mcp.config;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.tool.AdminFixedTools;
import cn.shopex.ecshopx.mcp.tool.HttpToolDeclaration;
import cn.shopex.ecshopx.mcp.tool.LoopbackToolExecutor;
import cn.shopex.ecshopx.mcp.tool.ToolDeclarationRegistrar;
import cn.shopex.ecshopx.mcp.tool.grant.MemberCouponGrantToolCallback;
import cn.shopex.ecshopx.mcp.tool.smoke.McpSmokeTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import cn.shopex.ecshopx.mcp.auth.McpBearerAuthFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(McpProperties.class)
public class McpServerConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ApiLoopbackClient apiLoopbackClient(McpProperties properties, RestClient.Builder restClientBuilder) {
		return new ApiLoopbackClient(properties, restClientBuilder);
	}

	@Bean
	@ConditionalOnMissingBean
	LoopbackToolExecutor loopbackToolExecutor(ApiLoopbackClient apiLoopbackClient, ObjectMapper objectMapper) {
		return new LoopbackToolExecutor(apiLoopbackClient, objectMapper);
	}

	@Bean
	ToolCallbackProvider mcpLoopbackToolCallbackProvider(
			LoopbackToolExecutor loopbackToolExecutor, McpProperties mcpProperties) {
		List<HttpToolDeclaration> declarations = new ArrayList<>();
		declarations.add(McpSmokeTools.smokeDeclaration(mcpProperties));
		declarations.addAll(AdminFixedTools.allDeclarations());
		List<ToolCallback> callbacks = new ArrayList<>();
		for (HttpToolDeclaration declaration : declarations) {
			callbacks.add(ToolDeclarationRegistrar.toToolCallback(declaration, loopbackToolExecutor));
		}
		callbacks.add(new MemberCouponGrantToolCallback(loopbackToolExecutor, mcpProperties));
		return new StaticToolCallbackProvider(callbacks.toArray(ToolCallback[]::new));
	}

	@Bean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	FilterRegistrationBean<McpBearerAuthFilter> mcpBearerAuthFilter(McpProperties mcpProperties) {
		FilterRegistrationBean<McpBearerAuthFilter> registration = new FilterRegistrationBean<>(
				new McpBearerAuthFilter(mcpProperties));
		String pattern = mcpProperties.getMcpPath();
		if (!pattern.startsWith("/")) {
			pattern = "/" + pattern;
		}
		registration.addUrlPatterns(pattern);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
		return registration;
	}
}