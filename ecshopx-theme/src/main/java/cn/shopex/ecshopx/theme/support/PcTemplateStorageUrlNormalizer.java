/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.theme.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Aligns with PHP {@code ThemePcTemplateContentServices::normalizeStorageUrlsForCurrentAppUrl}: rewrite local
 * {@code /storage/...} absolute URLs to the current app origin.
 */
@Component
public class PcTemplateStorageUrlNormalizer {

	private static final Pattern STORAGE_URL_PATTERN =
			Pattern.compile("https?://[^\\s\"'<>()]+/storage/[^\\s\"'<>()]+");

	private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

	private final ObjectMapper objectMapper;
	private final String appUrl;

	public PcTemplateStorageUrlNormalizer(
			ObjectMapper objectMapper, @Value("${APP_URL:}") String appUrl) {
		this.objectMapper = objectMapper;
		this.appUrl = appUrl == null ? "" : appUrl;
	}

	public String normalize(String config) {
		return normalize(config, appUrl);
	}

	public String normalize(String config, String currentAppUrl) {
		if (!StringUtils.hasText(config)) {
			return config;
		}
		String currentOrigin = normalizeAppOrigin(currentAppUrl);
		if (!StringUtils.hasText(currentOrigin)) {
			return config;
		}
		try {
			JsonNode root = objectMapper.readTree(config);
			if (root != null && (root.isObject() || root.isArray())) {
				JsonNode rewritten = normalizeStorageUrlsInValue(root, currentOrigin);
				return objectMapper.writeValueAsString(rewritten);
			}
		} catch (JsonProcessingException ignored) {
			// fall through to string rewrite
		}
		return normalizeStorageUrlsInString(config, currentOrigin);
	}

	private JsonNode normalizeStorageUrlsInValue(JsonNode value, String currentOrigin) {
		if (value == null || value.isNull()) {
			return value;
		}
		if (value.isObject()) {
			ObjectNode obj = (ObjectNode) value;
			ObjectNode out = objectMapper.createObjectNode();
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				Map.Entry<String, JsonNode> e = it.next();
				out.set(e.getKey(), normalizeStorageUrlsInValue(e.getValue(), currentOrigin));
			}
			return out;
		}
		if (value.isArray()) {
			ArrayNode arr = (ArrayNode) value;
			ArrayNode out = objectMapper.createArrayNode();
			for (JsonNode el : arr) {
				out.add(normalizeStorageUrlsInValue(el, currentOrigin));
			}
			return out;
		}
		if (value.isTextual()) {
			return TextNode.valueOf(normalizeStorageUrlsInString(value.asText(), currentOrigin));
		}
		return value;
	}

	private static String normalizeStorageUrlsInString(String value, String currentOrigin) {
		if (!StringUtils.hasText(value)) {
			return value;
		}
		Matcher m = STORAGE_URL_PATTERN.matcher(value);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, Matcher.quoteReplacement(normalizeOneStorageUrl(m.group(), currentOrigin)));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String normalizeOneStorageUrl(String url, String currentOrigin) {
		try {
			URI oldParts = URI.create(url);
			URI currentParts = URI.create(currentOrigin);
			String oldHost = oldParts.getHost();
			String path = oldParts.getRawPath();
			String currentHost = currentParts.getHost();
			if (!StringUtils.hasText(oldHost) || !StringUtils.hasText(path) || !StringUtils.hasText(currentHost)) {
				return url;
			}
			if (!path.startsWith("/storage/")) {
				return url;
			}
			String oldHostLower = oldHost.toLowerCase();
			String currentHostLower = currentHost.toLowerCase();
			if (!LOCAL_HOSTS.contains(oldHostLower) && !oldHostLower.equals(currentHostLower)) {
				return url;
			}
			StringBuilder next = new StringBuilder();
			next.append(trimTrailingSlash(currentOrigin)).append(path);
			if (oldParts.getRawQuery() != null) {
				next.append('?').append(oldParts.getRawQuery());
			}
			if (oldParts.getRawFragment() != null) {
				next.append('#').append(oldParts.getRawFragment());
			}
			return next.toString();
		} catch (IllegalArgumentException ex) {
			return url;
		}
	}

	static String normalizeAppOrigin(String url) {
		String raw = url == null ? "" : url.trim();
		if (raw.isEmpty()) {
			return "";
		}
		try {
			URI parts = URI.create(raw);
			if (!StringUtils.hasText(parts.getScheme()) || !StringUtils.hasText(parts.getHost())) {
				return trimTrailingSlash(raw);
			}
			StringBuilder origin = new StringBuilder();
			origin.append(parts.getScheme()).append("://").append(parts.getHost());
			if (parts.getPort() > 0) {
				origin.append(':').append(parts.getPort());
			}
			return origin.toString();
		} catch (IllegalArgumentException ex) {
			return trimTrailingSlash(raw);
		}
	}

	private static String trimTrailingSlash(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
	}
}
