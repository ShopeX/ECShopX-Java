package cn.shopex.ecshopx.mcp.tool.catalog;

import java.util.List;

/** Test-only catalog factory (same package as {@link MenuApiCatalog}). */
public final class MenuApiCatalogs {

	private MenuApiCatalogs() {
	}

	public static MenuApiCatalog of(List<MenuApiEntry> entries) {
		return new MenuApiCatalog(entries);
	}
}