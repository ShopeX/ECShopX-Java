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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2NormalItemsEntityDetailService {

	private final ItemsRepository itemsRepository;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public OpenapiThirdApiV2NormalItemsEntityDetailService(
			ItemsRepository itemsRepository,
			PlatformItemsDetailCoreService platformItemsDetailCoreService,
			ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsRepository = itemsRepository;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	public Map<String, Object> executeOpenapiGetEntityDetail(long companyId, String itemBnRaw) {
		validateItemBn(itemBnRaw);

		Items defaultSku = itemsRepository.findDefaultByItemBnAndCompany(itemBnRaw.trim(), companyId);
		if (defaultSku == null || defaultSku.getItemId() == null) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, "商品找不到");
		}
		long itemId = defaultSku.getItemId();

		Map<String, Object> detail;
		try {
			detail = platformItemsDetailCoreService.build(companyId, itemId, null);
		} catch (RuntimeException ex) {
			throw new OpenapiItemsV2FailException("E5000", ex.getMessage());
		}
		if (detail == null || detail.isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, "商品找不到");
		}

		itemsListMultiLangApplier.applyToRows(companyId, "zh-CN", List.of(detail));

		return formatItemSpuDetail(detail);
	}

	private static void validateItemBn(String itemBnRaw) {
		if (itemBnRaw == null || itemBnRaw.trim().isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "货号必填");
		}
	}

	private static Map<String, Object> formatItemSpuDetail(Map<String, Object> data) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("goods_id", data.get("goods_id"));
		result.put("item_bn", data.get("item_bn"));
		result.put("item_name", data.get("item_name"));
		result.put("brief", data.get("brief"));
		result.put("item_unit", data.get("item_unit"));
		result.put("sort", data.get("sort"));
		result.put("brand_id", data.get("brand_id"));
		result.put("templates_id", data.get("templates_id"));
		result.put("is_gift", data.get("is_gift"));
		result.put("pics", data.get("pics"));
		result.put("nospec", data.get("nospec"));
		result.put("is_show_specimg", data.get("is_show_specimg"));
		result.put("weight", data.get("weight"));
		result.put("volume", data.get("volume"));
		result.put("price", data.get("price"));
		result.put("market_price", data.get("market_price"));
		result.put("cost_price", data.get("cost_price"));
		result.put("barcode", data.get("barcode"));
		result.put("approve_status", data.get("approve_status"));
		result.put("store", data.get("store"));
		result.put("item_main_cat_id", data.get("item_main_cat_id"));
		result.put("item_category", data.get("item_category"));
		result.put("update_time", data.get("updated"));

		Object nospec = data.get("nospec");
		boolean multiSpec = nospec instanceof Boolean b ? !b
				: "false".equalsIgnoreCase(String.valueOf(nospec).trim());
		if (multiSpec) {
			result.put("spec_items", formatItemSpec(castList(data.get("spec_items"))));
			result.put("item_spec_desc", formatItemSpecDesc(castList(data.get("item_spec_desc"))));
			result.put("spec_images", formatSpecImages(castList(data.get("spec_images"))));
		}
		return result;
	}

	private static List<Map<String, Object>> formatSpecImages(List<Map<String, Object>> specImages) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> images : specImages) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("spec_value_id", images.get("spec_value_id"));
			row.put("spec_custom_value_name", images.get("spec_custom_value_name"));
			row.put("spec_value_name", images.get("spec_value_name"));
			row.put("spec_image_url", images.get("spec_image_url"));
			result.add(row);
		}
		return result;
	}

	private static List<Map<String, Object>> formatItemSpecDesc(List<Map<String, Object>> itemSpecDesc) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> spec : itemSpecDesc) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("spec_id", spec.get("spec_id"));
			row.put("spec_name", spec.get("spec_name"));
			row.put("is_image", spec.get("is_image"));
			row.put("spec_values", formatSpecValues(castList(spec.get("spec_values"))));
			result.add(row);
		}
		return result;
	}

	private static List<Map<String, Object>> formatSpecValues(List<Map<String, Object>> specValues) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> values : specValues) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("spec_value_id", values.get("spec_value_id"));
			row.put("spec_value_name", values.get("spec_value_name"));
			row.put("spec_custom_value_name", values.get("spec_custom_value_name"));
			row.put("spec_image_url", values.get("spec_image_url"));
			result.add(row);
		}
		return result;
	}

	private static List<Map<String, Object>> formatItemSpec(List<Map<String, Object>> specItems) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> spec : specItems) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("item_bn", spec.get("item_bn"));
			row.put("is_default", spec.get("is_default"));
			row.put("approve_status", spec.get("approve_status"));
			row.put("weight", spec.get("weight"));
			row.put("volume", spec.get("volume"));
			row.put("price", spec.get("price"));
			row.put("market_price", spec.get("market_price"));
			row.put("cost_price", spec.get("cost_price"));
			row.put("barcode", spec.get("barcode"));
			row.put("item_spec", formatSpec(castList(spec.get("item_spec"))));
			row.put("store", spec.get("store"));
			result.add(row);
		}
		return result;
	}

	private static List<Map<String, Object>> formatSpec(List<Map<String, Object>> spec) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map<String, Object> data : spec) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("spec_id", data.get("spec_id"));
			row.put("spec_value_id", data.get("spec_value_id"));
			row.put("spec_name", data.get("spec_name"));
			row.put("spec_custom_value_name", data.get("spec_custom_value_name"));
			row.put("spec_value_name", data.get("spec_value_name"));
			row.put("spec_image_url", data.get("spec_image_url"));
			result.add(row);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castList(Object raw) {
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					out.add((Map<String, Object>) m);
				}
			}
			return out;
		}
		return List.of();
	}
}
