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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.config.DigiOsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigiOsRegionMapper {

	private final DigiOsProperties properties;
	private final ResourceLoader resourceLoader;
	private final ObjectMapper objectMapper;

	private volatile boolean loaded;
	private volatile Map<String, RegionRow> byCode = Map.of();
	private volatile Map<String, Map<String, List<String>>> byNameAndType = Map.of();

	public DigiOsRegionMapper(DigiOsProperties properties, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
		this.objectMapper = objectMapper;
	}

	public String resolveAreaCd(Object regionsId, String province, String city, String area) {
		ensureLoaded();
		List<String> codes = parseRegionCodes(regionsId);
		if (!codes.isEmpty()) {
			String districtCode = codes.get(codes.size() - 1);
			if (byCode.containsKey(districtCode)) {
				return districtCode;
			}
		}
		area = trim(area);
		if (StringUtils.hasText(area)) {
			String matched = matchByName(area, List.of("区", "县", "市"), codes);
			if (matched != null) {
				return matched;
			}
		}
		city = trim(city);
		if (StringUtils.hasText(city)) {
			String matched = matchByName(city, List.of("市", "区", "县"), codes);
			if (matched != null) {
				return matched;
			}
		}
		province = trim(province);
		if (StringUtils.hasText(province)) {
			String matched = matchByName(province, List.of("省", "市", "自治区"), codes);
			if (matched != null) {
				return matched;
			}
		}
		return null;
	}

	public List<String> parseRegionCodes(Object regionsId) {
		List<String> parts = new ArrayList<>();
		if (regionsId instanceof List<?> list) {
			for (Object item : list) {
				parts.add(String.valueOf(item));
			}
		} else {
			String raw = trim(regionsId);
			if (!StringUtils.hasText(raw)) {
				return List.of();
			}
			if (raw.contains(",")) {
				for (String p : raw.split(",")) {
					parts.add(p);
				}
			} else {
				parts.add(raw);
			}
		}
		List<String> codes = new ArrayList<>();
		for (String part : parts) {
			String code = part.trim();
			if (StringUtils.hasText(code)) {
				codes.add(code);
			}
		}
		return codes;
	}

	private String matchByName(String name, List<String> types, List<String> regionCodes) {
		List<String> candidates = new ArrayList<>();
		for (String type : types) {
			Map<String, List<String>> byName = byNameAndType.getOrDefault(type, Map.of());
			for (String code : byName.getOrDefault(name, List.of())) {
				candidates.add(code);
			}
		}
		if (candidates.isEmpty()) {
			return null;
		}
		List<String> unique = candidates.stream().distinct().toList();
		if (unique.size() == 1) {
			return unique.get(0);
		}
		for (String code : unique) {
			if (parentChainContainsAny(code, regionCodes)) {
				return code;
			}
		}
		return unique.get(0);
	}

	private boolean parentChainContainsAny(String code, List<String> regionCodes) {
		if (regionCodes.isEmpty()) {
			return false;
		}
		RegionRow row = byCode.get(code);
		String parent = row == null ? "" : trim(row.parent);
		if (!StringUtils.hasText(parent)) {
			return false;
		}
		for (String regionCode : regionCodes) {
			if (StringUtils.hasText(regionCode) && parent.contains(regionCode)) {
				return true;
			}
		}
		return false;
	}

	private void ensureLoaded() {
		if (loaded) {
			return;
		}
		synchronized (this) {
			if (loaded) {
				return;
			}
			load();
			loaded = true;
		}
	}

	private void load() {
		try (InputStream in = openRegionsStream()) {
			if (in == null) {
				byCode = Map.of();
				byNameAndType = Map.of();
				return;
			}
			JsonNode root = objectMapper.readTree(in);
			JsonNode byCodeNode = root.get("by_code");
			Map<String, RegionRow> codeMap = new HashMap<>();
			Map<String, Map<String, List<String>>> nameTypeMap = new HashMap<>();
			if (byCodeNode != null && byCodeNode.isObject()) {
				byCodeNode.fields().forEachRemaining(entry -> {
					String code = entry.getKey();
					JsonNode row = entry.getValue();
					String type = row.path("type").asText("");
					String name = row.path("name").asText("");
					String parent = row.path("parent").asText("");
					if (!StringUtils.hasText(type) || !StringUtils.hasText(name)) {
						return;
					}
					codeMap.put(code, new RegionRow(name, type, parent));
					nameTypeMap
							.computeIfAbsent(type, k -> new LinkedHashMap<>())
							.computeIfAbsent(name, k -> new ArrayList<>())
							.add(code);
				});
			}
			byCode = codeMap;
			byNameAndType = nameTypeMap;
		} catch (Exception e) {
			byCode = Map.of();
			byNameAndType = Map.of();
		}
	}

	private InputStream openRegionsStream() throws Exception {
		String location = properties.getRegionsJson();
		if (!StringUtils.hasText(location)) {
			location = "classpath:digios_regions.json";
		}
		if (location.startsWith("classpath:")) {
			Resource resource = resourceLoader.getResource(location);
			if (resource.exists()) {
				return resource.getInputStream();
			}
			return null;
		}
		Path path = Path.of(location);
		if (Files.isReadable(path)) {
			return Files.newInputStream(path);
		}
		return null;
	}

	private record RegionRow(String name, String type, String parent) {}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
