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

package cn.shopex.ecshopx.espier.service.address;

import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EspierAddressTreeApplicationService {

	private static final String REDIS_KEY_ADDRESS = "address";

	private final AddressMapper addressMapper;
	private final StringRedisTemplate espierStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public EspierAddressTreeApplicationService(
			AddressMapper addressMapper,
			@Qualifier("espierStringRedisTemplate") StringRedisTemplate espierStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.addressMapper = addressMapper;
		this.espierStringRedisTemplate = espierStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> get() {
		String cached = espierStringRedisTemplate.opsForValue().get(REDIS_KEY_ADDRESS);
		if (cached == null || cached.isEmpty()) {
			return loadTreeFromDbAndWriteRedis();
		}
		try {
			JsonNode root = objectMapper.readTree(cached);
			if (root == null || root.isNull() || !root.isArray()) {
				espierStringRedisTemplate.delete(REDIS_KEY_ADDRESS);
				return loadTreeFromDbAndWriteRedis();
			}
			List<Map<String, Object>> list =
					objectMapper.convertValue(root, new TypeReference<List<Map<String, Object>>>() {});
			if (list == null) {
				espierStringRedisTemplate.delete(REDIS_KEY_ADDRESS);
				return loadTreeFromDbAndWriteRedis();
			}
			return normalizeAddressTree(list);
		} catch (JsonProcessingException | IllegalArgumentException ex) {
			espierStringRedisTemplate.delete(REDIS_KEY_ADDRESS);
			return loadTreeFromDbAndWriteRedis();
		}
	}

	private Map<String, Object> toNode(Address entity) {
		Map<String, Object> node = new LinkedHashMap<>();
		node.put("id", entity.getId());
		node.put("value", entity.getId());
		node.put("label", entity.getLabel());
		node.put("parent_id", entity.getParentId());
		node.put("path", entity.getPath());
		return node;
	}

	private List<Map<String, Object>> loadTreeFromDbAndWriteRedis() {
		List<Address> provinces =
				addressMapper.selectList(
						new LambdaQueryWrapper<Address>()
								.eq(Address::getParentId, 0L)
								.orderByAsc(Address::getId));
		if (provinces == null) {
			provinces = Collections.emptyList();
		}
		List<Map<String, Object>> provinceList = new ArrayList<>();
		for (Address p : provinces) {
			Map<String, Object> provNode = toNode(p);
			List<Address> cities =
					addressMapper.selectList(
							new LambdaQueryWrapper<Address>()
									.eq(Address::getParentId, p.getId())
									.orderByAsc(Address::getId));
			if (cities == null) {
				cities = Collections.emptyList();
			}
			List<Map<String, Object>> cityList = new ArrayList<>();
			for (Address city : cities) {
				Map<String, Object> cityNode = toNode(city);
				List<Address> districts =
						addressMapper.selectList(
								new LambdaQueryWrapper<Address>()
										.eq(Address::getParentId, city.getId())
										.orderByAsc(Address::getId));
				if (districts == null) {
					districts = Collections.emptyList();
				}
				List<Map<String, Object>> districtList = new ArrayList<>();
				for (Address d : districts) {
					districtList.add(toNode(d));
				}
				cityNode.put("children", districtList);
				cityList.add(cityNode);
			}
			provNode.put("children", cityList);
			provinceList.add(provNode);
		}
		try {
			String json = objectMapper.writeValueAsString(provinceList);
			espierStringRedisTemplate.opsForValue().set(REDIS_KEY_ADDRESS, json);
		} catch (JsonProcessingException ignored) {
			// Built from DB entities; failure to stringify should not block the response.
		}
		return normalizeAddressTree(provinceList);
	}

	private List<Map<String, Object>> normalizeAddressTree(List<Map<String, Object>> address) {
		for (Map<String, Object> node : address) {
			coerceNodeNumericTypes(node);
		}
		for (int k = 0; k < address.size(); k++) {
			Map<String, Object> node = address.get(k);
			Object childrenObj = node.get("children");
			if (childrenObj instanceof List<?> children && !children.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> cityChildren = (List<Map<String, Object>>) childrenObj;
				node.put("children", expandDirectCountyAddresses(cityChildren));
			}
		}
		return address;
	}

	private void coerceNodeNumericTypes(Map<String, Object> node) {
		node.put("id", toLong(node.get("id")));
		node.put("value", toLong(node.get("value")));
		node.put("parent_id", toLong(node.get("parent_id")));
		Object children = node.get("children");
		if (children instanceof List<?> list) {
			for (Object child : list) {
				if (child instanceof Map<?, ?> childMap) {
					@SuppressWarnings("unchecked")
					Map<String, Object> childNode = (Map<String, Object>) childMap;
					coerceNodeNumericTypes(childNode);
				}
			}
		}
	}

	private static Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(value.toString());
	}

	private List<Map<String, Object>> expandDirectCountyAddresses(
			List<Map<String, Object>> children) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> child : children) {
			if ("省直辖县级行政区划".equals(child.get("label"))) {
				Object grandChildrenObj = child.get("children");
				if (grandChildrenObj instanceof List<?> grandChildren
						&& !grandChildren.isEmpty()) {
					for (Object grandChildObj : grandChildren) {
						@SuppressWarnings("unchecked")
						Map<String, Object> grandChild = (Map<String, Object>) grandChildObj;
						Map<String, Object> expanded = new LinkedHashMap<>(child);
						expanded.put("label", grandChild.get("label"));
						expanded.put("children", List.of(grandChild));
						result.add(expanded);
					}
				} else {
					result.add(child);
				}
			} else {
				result.add(child);
			}
		}
		return result;
	}
}
