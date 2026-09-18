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

package cn.shopex.ecshopx.openapi.thirdapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMethodDescriptor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiMethodRegistry {

	public static final String PUBLIC_OPENAPI_PREFIX = "/api/openapi";
	private static final Pattern FULLNAME_PATTERN =
			Pattern.compile("^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\s+/api/openapi/(.+)$", Pattern.CASE_INSENSITIVE);

	private final Map<String, OpenapiMethodDescriptor> byRouteKey = new LinkedHashMap<>();

	public OpenapiMethodRegistry() {
		loadFromClasspath();
	}

	public Optional<OpenapiMethodDescriptor> lookup(String version, String httpVerb, String method) {
		if (!StringUtils.hasText(method)) {
			return Optional.empty();
		}
		String key = normalizeVersion(version) + " " + httpVerb.toUpperCase() + " " + method.trim();
		return Optional.ofNullable(byRouteKey.get(key));
	}

	public List<OpenapiMethodDescriptor> all() {
		return List.copyOf(byRouteKey.values());
	}

	int size() {
		return byRouteKey.size();
	}

	private void loadFromClasspath() {
		ClassPathResource resource = new ClassPathResource("openapi/openapi_list.csv");
		if (!resource.exists()) {
			return;
		}
		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			boolean header = true;
			while ((line = reader.readLine()) != null) {
				if (header) {
					header = false;
					continue;
				}
				if (!StringUtils.hasText(line)) {
					continue;
				}
				parseLine(line).ifPresent(d -> byRouteKey.put(d.routeKey(), d));
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load openapi_list.csv", e);
		}
	}

	static Optional<OpenapiMethodDescriptor> parseLine(String line) {
		List<String> cols = splitCsvLine(line);
		if (cols.size() < 10) {
			return Optional.empty();
		}
		String fullname = cols.get(0).trim();
		Matcher m = FULLNAME_PATTERN.matcher(fullname);
		if (!m.matches()) {
			return Optional.empty();
		}
		String httpVerb = m.group(1).toUpperCase();
		String method = m.group(2).trim();
		String javaClass = sanitize(cols.get(6));
		String javaVersion = sanitize(cols.get(8));
		String javaMethod = sanitize(cols.get(9));
		if (!StringUtils.hasText(method) || !StringUtils.hasText(javaVersion)) {
			return Optional.empty();
		}
		String version = javaVersion.startsWith("v") ? javaVersion.substring(1) + ".0" : javaVersion;
		return Optional.of(
				new OpenapiMethodDescriptor(method, httpVerb, version, javaVersion, javaClass, javaMethod));
	}

	private static String sanitize(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim().replace("'", "").replace("]", "").replace("[", "");
	}

	private static List<String> splitCsvLine(String line) {
		List<String> cols = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				inQuotes = !inQuotes;
				continue;
			}
			if (c == ',' && !inQuotes) {
				cols.add(cur.toString());
				cur.setLength(0);
				continue;
			}
			cur.append(c);
		}
		cols.add(cur.toString());
		return cols;
	}

	static String normalizeVersion(String version) {
		if (!StringUtils.hasText(version)) {
			return "1.0";
		}
		String s = version.trim();
		if (s.startsWith("v") || s.startsWith("V")) {
			s = s.substring(1);
		}
		if (s.matches("\\d+")) {
			return s + ".0";
		}
		return s;
	}
}
