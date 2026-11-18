package cn.shopex.ecshopx.mcp.tool.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class MenuApiCatalogLoader {

	private static final String RESOURCE = "/mcp/menu-api-catalog.json";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private MenuApiCatalogLoader() {
	}

	public static MenuApiCatalog loadClasspath() {
		try (InputStream in = MenuApiCatalogLoader.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			MenuApiCatalogDocument doc = MAPPER.readValue(in, MenuApiCatalogDocument.class);
			return new MenuApiCatalog(doc.entries());
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to load " + RESOURCE, e);
		}
	}

	private record MenuApiCatalogDocument(int version, List<MenuApiEntry> entries) {
		private MenuApiCatalogDocument {
			entries = entries == null ? List.of() : List.copyOf(entries);
		}
	}
}