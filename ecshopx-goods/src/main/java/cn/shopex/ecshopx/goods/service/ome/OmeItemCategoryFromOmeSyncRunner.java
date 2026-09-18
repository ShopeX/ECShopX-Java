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

package cn.shopex.ecshopx.goods.service.ome;

import cn.shopex.ecshopx.goods.service.ItemsCategorySaveService;
import cn.shopex.ecshopx.goods.service.dto.CategoryTreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OmeItemCategoryFromOmeSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(OmeItemCategoryFromOmeSyncRunner.class);

	private final ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor;
	private final ShopexErpOpenApiClient openApiClient;
	private final ItemsCategorySaveService itemsCategorySaveService;
	private final ObjectMapper objectMapper;

	public OmeItemCategoryFromOmeSyncRunner(
			ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor,
			ShopexErpOpenApiClient openApiClient,
			ItemsCategorySaveService itemsCategorySaveService,
			ObjectMapper objectMapper) {
		this.shopexErpSettingRedisAccessor = shopexErpSettingRedisAccessor;
		this.openApiClient = openApiClient;
		this.itemsCategorySaveService = itemsCategorySaveService;
		this.objectMapper = objectMapper;
	}

	public void consumeQueuedSync(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		int distributorId = payload.containsKey("distributor_id") ? toInt(payload.get("distributor_id")) : 0;

		Map<String, Object> setting = shopexErpSettingRedisAccessor.getParsedSetting(companyId);
		if (setting == null || !Boolean.TRUE.equals(setting.get("is_openapi_open"))) {
			log.debug("companyId:{},msg:未开启OME开放数据接口", companyId);
			return;
		}

		String method = "category.getList";
		Map<String, Object> requestDataSnapshot = new LinkedHashMap<>();
		Map<String, Object> resultSnapshot = new LinkedHashMap<>();

		try {
			Map<String, Object> bizParams = new LinkedHashMap<>();
			Map<String, Object> result = openApiClient.call(companyId, method, bizParams);
			resultSnapshot = result;

			if (result == null || !"succ".equals(result.get("rsp"))) {
				log.debug("companyId:{},msg:OME批量获取商品分类信息请求失败", companyId);
				return;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) result.get("data");
			if (data == null) {
				data = new LinkedHashMap<>();
			}
			int count = toInt(data.get("count"));
			List<Map<String, Object>> lists = coerceCategoryLists(data.get("lists"));

			if (count > 0 && !lists.isEmpty()) {
				List<CategoryTreeNode> roots = buildCategoryTreeFromOmeFlatList(lists, objectMapper);
				itemsCategorySaveService.saveItemsCategory(companyId, distributorId, roots);
			}

			try {
				log.debug("{}=>requestData:{}==>result:\r\n{}",
						method,
						objectMapper.writeValueAsString(requestDataSnapshot),
						objectMapper.writeValueAsString(result));
			} catch (Exception logEx) {
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, requestDataSnapshot, result);
			}
		} catch (Exception e) {
			try {
				log.debug("OME请求失败:{}=>method:{}=>requestData:{}=>result:{}",
						e.getMessage(),
						method,
						objectMapper.writeValueAsString(requestDataSnapshot),
						objectMapper.writeValueAsString(resultSnapshot));
			} catch (Exception jsonEx) {
				log.debug("OME请求失败:{}=>method:{}=>requestData:{}=>result:{}",
						e.getMessage(), method, requestDataSnapshot, resultSnapshot);
			}
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> coerceCategoryLists(Object listsObj) {
		if (!(listsObj instanceof List<?> raw)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : raw) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(row);
			}
		}
		return out;
	}

	private static List<CategoryTreeNode> buildCategoryTreeFromOmeFlatList(
			List<Map<String, Object>> array, ObjectMapper objectMapper) {
		return getTreeFromOmeFlat(array, "", objectMapper);
	}

	private static List<CategoryTreeNode> getTreeFromOmeFlat(
			List<Map<String, Object>> array, String pid, ObjectMapper objectMapper) {
		List<CategoryTreeNode> list = new ArrayList<>();
		for (Map<String, Object> v : array) {
			if (!Objects.equals(normalizeParentCode(v.get("parent_code")), pid)) {
				continue;
			}
			CategoryTreeNode node = new CategoryTreeNode();
			Object catName = v.get("cat_name");
			node.setCategoryName(catName == null ? "" : String.valueOf(catName));
			Object catCode = v.get("cat_code");
			node.setCategoryCode(catCode == null ? null : String.valueOf(catCode));
			node.setIsMainCategory(true);
			node.setSort(0L);
			JsonNode emptyArray = objectMapper.createArrayNode();
			node.setGoodsParams(emptyArray);
			node.setGoodsSpec(emptyArray);
			node.setImageUrl("");
			node.setChildren(new ArrayList<>());

			Object pathObj = v.get("cat_code_path");
			String catCodePath = pathObj == null ? "" : String.valueOf(pathObj);
			String[] parts = catCodePath.split(",", -1);
			int segmentCount = parts.length;
			if (segmentCount < 3) {
				String childPid = normalizeParentCode(v.get("cat_code"));
				node.getChildren().addAll(getTreeFromOmeFlat(array, childPid, objectMapper));
			}
			list.add(node);
		}
		return list;
	}

	private static String normalizeParentCode(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
