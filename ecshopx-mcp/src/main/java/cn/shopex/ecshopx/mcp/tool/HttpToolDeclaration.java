package cn.shopex.ecshopx.mcp.tool;

public record HttpToolDeclaration(
		String name,
		String description,
		String inputSchemaJson,
		String method,
		String pathTemplate) {
}