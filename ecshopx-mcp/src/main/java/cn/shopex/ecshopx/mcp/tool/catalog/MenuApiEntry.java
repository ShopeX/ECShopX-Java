package cn.shopex.ecshopx.mcp.tool.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public record MenuApiEntry(
		List<String> routes,
		List<String> permissions,
		String method,
		String path,
		String risk,
		@JsonProperty("params") List<String> paramHints) {

	public MenuApiEntry {
		routes = routes == null ? List.of() : List.copyOf(routes);
		permissions = permissions == null ? List.of() : List.copyOf(permissions);
		paramHints = paramHints == null ? List.of() : List.copyOf(paramHints);
	}

	boolean matchesRouteOrPermissionKey(String needle) {
		return routes.stream().anyMatch(r -> r.equalsIgnoreCase(needle))
				|| permissions.stream().anyMatch(p -> p.equalsIgnoreCase(needle))
				|| path.equalsIgnoreCase(needle);
	}

	boolean matchesQuery(String query) {
		String q = query.toLowerCase();
		if (path.toLowerCase().contains(q)) {
			return true;
		}
		return routes.stream().anyMatch(r -> r.toLowerCase().contains(q))
				|| permissions.stream().anyMatch(p -> p.toLowerCase().contains(q));
	}

	public boolean intersectsMenuKeys(Set<String> menuKeys) {
		if (menuKeys == null || menuKeys.isEmpty()) {
			return false;
		}
		for (String key : menuKeys) {
			if (routes.stream().anyMatch(r -> r.equalsIgnoreCase(key))
					|| permissions.stream().anyMatch(p -> p.equalsIgnoreCase(key))) {
				return true;
			}
		}
		return false;
	}
}