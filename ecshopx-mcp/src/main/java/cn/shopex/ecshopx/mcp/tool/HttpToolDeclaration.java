package cn.shopex.ecshopx.mcp.tool;

import java.util.Set;

public record HttpToolDeclaration(
		String name,
		String description,
		String inputSchemaJson,
		String method,
		String pathTemplate,
		boolean requireConfirm,
		Set<String> requiredArgs) {

	public HttpToolDeclaration {
		requiredArgs = requiredArgs == null ? Set.of() : Set.copyOf(requiredArgs);
	}

	/** 只读便捷构造（无 confirm、无额外必填）。 */
	public static HttpToolDeclaration read(
			String name, String description, String inputSchemaJson, String method, String pathTemplate) {
		return new HttpToolDeclaration(name, description, inputSchemaJson, method, pathTemplate, false, Set.of());
	}
}