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

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.goods.dispatch.InventoryQueryFromJushuitanJobDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanQueryInventoryOrchestratorService {

	private static final int PAGE_SIZE = 100;

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final InventoryQueryFromJushuitanJobDispatchPublisher inventoryQueryFromJushuitanJobDispatchPublisher;

	public JushuitanQueryInventoryOrchestratorService(
			ItemsListQueryRepository itemsListQueryRepository,
			InventoryQueryFromJushuitanJobDispatchPublisher inventoryQueryFromJushuitanJobDispatchPublisher) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.inventoryQueryFromJushuitanJobDispatchPublisher = inventoryQueryFromJushuitanJobDispatchPublisher;
	}

	public void run(long companyId, Map<String, Object> mergedInput, HttpServletRequest request) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, Long.valueOf(companyId));
		filter.put("item_type", "normal");
		filter.put("audit_status", "approved");

		Object itemIdRaw = mergedInput != null ? mergedInput.get("item_id") : null;
		if (itemIdRaw == null && request != null) {
			String qp = request.getParameter("item_id");
			if (StringUtils.hasText(qp)) {
				itemIdRaw = qp.trim();
			}
		}
		if (ValuePresence.hasEffectiveValue(itemIdRaw)) {
			long singleId = parsePositiveLong(itemIdRaw);
			filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, List.of(singleId));
		}

		long totalCount = itemsListQueryRepository.countByParams(filter);
		int page = 1;
		do {
			int offset = (page - 1) * PAGE_SIZE;
			List<Items> list = itemsListQueryRepository.selectPageByParamsItemIdDesc(filter, offset, PAGE_SIZE);
			if (list != null && !list.isEmpty()) {
				List<Long> itemIds = new ArrayList<>();
				for (Items row : list) {
					Long iid = row.getItemId();
					if (iid != null && iid > 0) {
						itemIds.add(iid);
					}
				}
				if (!itemIds.isEmpty()) {
					inventoryQueryFromJushuitanJobDispatchPublisher.enqueueInventoryQueryFromJushuitan(
							companyId, itemIds);
				}
			}
			page++;
		} while ((long) (page - 1) * PAGE_SIZE < totalCount);
	}

	private static long parsePositiveLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(raw.toString().trim());
	}
}
