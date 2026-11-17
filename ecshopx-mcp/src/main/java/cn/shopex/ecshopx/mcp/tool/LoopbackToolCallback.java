package cn.shopex.ecshopx.mcp.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.StringUtils;

public final class LoopbackToolCallback implements ToolCallback {

	private final HttpToolDeclaration declaration;
	private final LoopbackToolExecutor executor;
	private final ToolDefinition toolDefinition;

	public LoopbackToolCallback(HttpToolDeclaration declaration, LoopbackToolExecutor executor) {
		this.declaration = declaration;
		this.executor = executor;
		this.toolDefinition = DefaultToolDefinition.builder()
				.name(declaration.name())
				.description(declaration.description())
				.inputSchema(declaration.inputSchemaJson())
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
		String argsJson = normalizeArgs(toolInput);
		ToolResult result = executor.execute(declaration, argsJson, ToolAuthContext.getBearer());
		return result.text();
	}

	private static String normalizeArgs(String toolInput) {
		if (!StringUtils.hasText(toolInput)) {
			return "{}";
		}
		return toolInput.trim();
	}
}