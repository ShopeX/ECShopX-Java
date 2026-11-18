package cn.shopex.ecshopx.mcp.tool.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MenuApiCatalog {

	private final List<MenuApiEntry> entries;

	MenuApiCatalog(List<MenuApiEntry> entries) {
		this.entries = List.copyOf(entries);
	}

	public List<MenuApiEntry> entries() {
		return entries;
	}

	public List<MenuApiEntry> findByRouteOrPermission(String needle) {
		if (needle == null || needle.isBlank()) {
			return List.of();
		}
		List<MenuApiEntry> hits = new ArrayList<>();
		for (MenuApiEntry entry : entries) {
			if (entry.matchesRouteOrPermissionKey(needle)) {
				hits.add(entry);
			}
		}
		return List.copyOf(hits);
	}

	public List<MenuApiEntry> findByMethodAndPath(String method, String path) {
		if (method == null || path == null) {
			return List.of();
		}
		String m = method.toUpperCase(Locale.ROOT);
		List<MenuApiEntry> hits = new ArrayList<>();
		for (MenuApiEntry entry : entries) {
			if (entry.method().equalsIgnoreCase(m) && entry.path().equalsIgnoreCase(path)) {
				hits.add(entry);
			}
		}
		return List.copyOf(hits);
	}

	public List<MenuApiEntry> resolve(String query, Set<String> menuKeys) {
		if (query == null || query.isBlank() || menuKeys == null || menuKeys.isEmpty()) {
			return List.of();
		}
		List<MenuApiEntry> hits = new ArrayList<>();
		for (MenuApiEntry entry : entries) {
			if (entry.matchesQuery(query) && entry.intersectsMenuKeys(menuKeys)) {
				hits.add(entry);
			}
		}
		return List.copyOf(hits);
	}
}