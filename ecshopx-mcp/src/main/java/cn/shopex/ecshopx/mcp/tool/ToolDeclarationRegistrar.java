package cn.shopex.ecshopx.mcp.tool;

import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

public final class ToolDeclarationRegistrar {

	private ToolDeclarationRegistrar() {
	}

	public static ToolCallback toToolCallback(HttpToolDeclaration declaration, LoopbackToolExecutor executor) {
		return new LoopbackToolCallback(declaration, executor);
	}

	public static ToolCallbackProvider provider(List<HttpToolDeclaration> declarations, LoopbackToolExecutor executor) {
		ToolCallback[] callbacks = declarations.stream()
				.map(declaration -> toToolCallback(declaration, executor))
				.toArray(ToolCallback[]::new);
		return new StaticToolCallbackProvider(callbacks);
	}
}