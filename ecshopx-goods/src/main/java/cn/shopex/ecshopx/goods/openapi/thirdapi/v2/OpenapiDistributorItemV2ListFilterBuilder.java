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

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiDistributorItemV2ListFilterBuilder {

	private final ItemsListQueryRepository itemsListQueryRepository;

	public OpenapiDistributorItemV2ListFilterBuilder(ItemsListQueryRepository itemsListQueryRepository) {
		this.itemsListQueryRepository = itemsListQueryRepository;
	}

	public BuildResult build(
			long companyId,
			String itemCodeRaw,
			String itemNameRaw,
			String goodsCanSaleRaw,
			String isTotalStoreRaw,
			String statusRaw) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		filter.put(ItemsListQueryRepository.KEY_IS_DEFAULT_EQ, 1);
		filter.put("item_type", "normal");

		if (isPresentNonEmpty(itemCodeRaw)) {
			filter.put("item_bn", itemCodeRaw);
		}
		if (itemNameRaw != null) {
			if (!itemNameRaw.isEmpty()) {
				List<Long> ids =
						itemsListQueryRepository.listDefaultItemIdsByItemNameContainsOnly(
								companyId, itemNameRaw);
				if (ids.isEmpty()) {
					return new BuildResult(filter, true);
				}
				filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, ids);
			}
		}
		if (isPresentNonEmpty(statusRaw)) {
			filter.put("approve_status", statusRaw);
		}

		return new BuildResult(filter, false);
	}

	private static boolean isPresentNonEmpty(String raw) {
		return raw != null && !raw.isEmpty();
	}

	public record BuildResult(Map<String, Object> filter, boolean emptyEarly) {}
}
