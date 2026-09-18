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

package cn.shopex.ecshopx.goods.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiProductSkuListPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiProductSkuListPortImpl implements OpenapiProductSkuListPort {

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public OpenapiProductSkuListPortImpl(
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	@Override
	public Map<String, Object> listNormalSkus(long companyId, int page, int pageSize, String countryCode) {
		int normalizedPage = Math.max(page, 1);
		int normalizedPageSize = pageSize > 2000 ? 2000 : pageSize;
		if (pageSize <= 0) {
			normalizedPageSize = 100;
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		filter.put("item_type", "normal");

		long totalCount = itemsListQueryRepository.countByParams(filter);
		List<Map<String, Object>> rowMaps = List.of();
		if (totalCount > 0) {
			int offset = (normalizedPage - 1) * normalizedPageSize;
			List<Items> rows = itemsListQueryRepository.selectPageByParamsItemIdDesc(filter, offset, normalizedPageSize);
			rowMaps = new ArrayList<>(rows.size());
			for (Items row : rows) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("item_id", row.getItemId());
				m.put("item_name", row.getItemName());
				m.put("item_bn", row.getItemBn());
				rowMaps.add(m);
			}
			String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
			itemsListMultiLangApplier.applyToRows(companyId, lang, rowMaps);
		}

		List<Map<String, Object>> skuList = new ArrayList<>();
		if (!rowMaps.isEmpty()) {
			for (Map<String, Object> row : rowMaps) {
				Map<String, Object> sku = new LinkedHashMap<>();
				sku.put("name", row.get("item_name"));
				sku.put("sku_id", row.get("item_bn"));
				skuList.add(sku);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("page", normalizedPage);
		data.put("page_size", normalizedPageSize);
		data.put("total_count", totalCount);
		data.put("sku_list", skuList);
		return data;
	}
}
