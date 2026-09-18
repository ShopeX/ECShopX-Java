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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.RegionsThird;
import cn.shopex.ecshopx.bspay.mapper.RegionsThirdMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BsPayRegionsListService {

	private static final String REDIS_KEY_BSPAY_REGIONS = "bspay_regions";
	private static final String REDIS_KEY_BSPAY_REGIONS_THIRD = "bspay_regions_third";

	private final RegionsThirdMapper regionsThirdMapper;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper regionsRedisObjectMapper;

	public BsPayRegionsListService(
			RegionsThirdMapper regionsThirdMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.regionsThirdMapper = regionsThirdMapper;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.regionsRedisObjectMapper = objectMapper
				.copy()
				.disable(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII);
	}

	public List<Map<String, Object>> getRegions() {
		String raw = sharedStringRedisTemplate.opsForValue().get(REDIS_KEY_BSPAY_REGIONS);
		if (raw != null && !raw.isBlank()) {
			try {
				List<Map<String, Object>> cached =
						regionsRedisObjectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
				if (cached != null && !cached.isEmpty()) {
					return cached;
				}
			} catch (JsonProcessingException ignored) {
				// fall through to DB
			}
		}

		List<Map<String, Object>> regions = new ArrayList<>();
		List<RegionsThird> provinces =
				regionsThirdMapper.selectList(new LambdaQueryWrapper<RegionsThird>().eq(RegionsThird::getPid, 0L));

		if (provinces.isEmpty()) {
			writeRegionsCache(regions);
			return regions;
		}

		List<Long> pids = provinces.stream().map(RegionsThird::getId).filter(Objects::nonNull).toList();
		List<RegionsThird> cities =
				regionsThirdMapper.selectList(new LambdaQueryWrapper<RegionsThird>().in(RegionsThird::getPid, pids));
		Map<Long, List<RegionsThird>> byParent =
				cities.stream().collect(Collectors.groupingBy(RegionsThird::getPid));

		for (RegionsThird province : provinces) {
			Map<String, Object> row = toSnakeRow(province);
			List<Map<String, Object>> children = new ArrayList<>();
			for (RegionsThird city : byParent.getOrDefault(province.getId(), Collections.emptyList())) {
				children.add(toSnakeRow(city));
			}
			row.put("children", children);
			regions.add(row);
		}

		writeRegionsCache(regions);
		return regions;
	}

	public List<Map<String, Object>> getRegionsThird() {
		String raw = sharedStringRedisTemplate.opsForValue().get(REDIS_KEY_BSPAY_REGIONS_THIRD);
		if (raw != null && !raw.isBlank()) {
			try {
				List<Map<String, Object>> cached =
						regionsRedisObjectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
				if (cached != null && !cached.isEmpty()) {
					return cached;
				}
			} catch (JsonProcessingException ignored) {
				// fall through to DB
			}
		}

		List<RegionsThird> provinces =
				regionsThirdMapper.selectList(new LambdaQueryWrapper<RegionsThird>().eq(RegionsThird::getPid, 0L));

		List<Map<String, Object>> regions = new ArrayList<>();
		if (provinces.isEmpty()) {
			writeRegionsThirdCache(regions);
			return regions;
		}

		List<Long> provinceIds = provinces.stream().map(RegionsThird::getId).filter(Objects::nonNull).toList();
		List<RegionsThird> allCities =
				regionsThirdMapper.selectList(new LambdaQueryWrapper<RegionsThird>().in(RegionsThird::getPid, provinceIds));
		Map<Long, List<RegionsThird>> citiesByProvincePid =
				allCities.stream().collect(Collectors.groupingBy(RegionsThird::getPid));

		List<Long> cityIds = allCities.stream().map(RegionsThird::getId).filter(Objects::nonNull).toList();
		final Map<Long, List<RegionsThird>> districtsByCityPid;
		if (cityIds.isEmpty()) {
			districtsByCityPid = Collections.emptyMap();
		} else {
			List<RegionsThird> allDistricts =
					regionsThirdMapper.selectList(new LambdaQueryWrapper<RegionsThird>().in(RegionsThird::getPid, cityIds));
			districtsByCityPid = allDistricts.stream().collect(Collectors.groupingBy(RegionsThird::getPid));
		}

		for (RegionsThird province : provinces) {
			Map<String, Object> prow = toSnakeRow(province);
			List<Map<String, Object>> cityChildren = new ArrayList<>();
			for (RegionsThird city : citiesByProvincePid.getOrDefault(province.getId(), Collections.emptyList())) {
				Map<String, Object> crow = toSnakeRow(city);
				List<Map<String, Object>> distChildren = new ArrayList<>();
				for (RegionsThird district : districtsByCityPid.getOrDefault(city.getId(), Collections.emptyList())) {
					distChildren.add(toSnakeRow(district));
				}
				crow.put("children", distChildren);
				cityChildren.add(crow);
			}
			prow.put("children", cityChildren);
			regions.add(prow);
		}

		writeRegionsThirdCache(regions);
		return regions;
	}

	private void writeRegionsThirdCache(List<Map<String, Object>> regions) {
		try {
			String json = regionsRedisObjectMapper.writeValueAsString(regions);
			sharedStringRedisTemplate.opsForValue().set(REDIS_KEY_BSPAY_REGIONS_THIRD, json);
		} catch (JsonProcessingException ignored) {
			// skip SET; still return regions
		}
	}

	private void writeRegionsCache(List<Map<String, Object>> regions) {
		try {
			String json = regionsRedisObjectMapper.writeValueAsString(regions);
			sharedStringRedisTemplate.opsForValue().set(REDIS_KEY_BSPAY_REGIONS, json);
		} catch (JsonProcessingException ignored) {
			// skip SET; still return regions
		}
	}

	private static Map<String, Object> toSnakeRow(RegionsThird e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("area_name", e.getAreaName());
		m.put("pid", e.getPid());
		m.put("area_code", e.getAreaCode());
		return m;
	}
}
