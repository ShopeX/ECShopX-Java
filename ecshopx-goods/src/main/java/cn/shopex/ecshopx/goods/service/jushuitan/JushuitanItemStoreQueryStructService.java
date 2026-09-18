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

package cn.shopex.ecshopx.goods.service.jushuitan;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanItemStoreQueryStructService {

	private final ItemsListQueryRepository itemsListQueryRepository;

	public JushuitanItemStoreQueryStructService(ItemsListQueryRepository itemsListQueryRepository) {
		this.itemsListQueryRepository = itemsListQueryRepository;
	}

	/**
	 * @return 供 JushuitanOpenApiClient item_store_query 的 biz 载荷；无法构建时返回 null
	 */
	@Nullable
	public Map<String, Object> buildItemStoreQueryPayload(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return null;
		}
		Map<String, Object> p = new LinkedHashMap<>();
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, Long.valueOf(companyId));
		p.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(itemIds));
		List<Items> rows = itemsListQueryRepository.selectPageByParamsItemIdDesc(p, 0, 20);
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		List<String> bns = new ArrayList<>();
		for (Items it : rows) {
			String bn = it.getItemBn();
			if (StringUtils.hasText(bn)) {
				bns.add(bn.trim());
			}
		}
		if (bns.isEmpty()) {
			return null;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("wms_co_id", 0);
		payload.put("page_index", 1);
		payload.put("page_size", 20);
		payload.put("sku_ids", String.join(",", bns));
		return payload;
	}
}
