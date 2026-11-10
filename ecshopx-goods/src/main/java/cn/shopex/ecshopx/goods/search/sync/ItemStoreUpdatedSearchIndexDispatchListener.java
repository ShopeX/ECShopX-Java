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

package cn.shopex.ecshopx.goods.search.sync;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.goods.ItemCompanyIdResolver;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 库存变更（event:258）→ 提交后增量 upsert 搜索索引。 */
@Component
public class ItemStoreUpdatedSearchIndexDispatchListener implements DispatchListener {

	private static final Logger log = LoggerFactory.getLogger(ItemStoreUpdatedSearchIndexDispatchListener.class);

	private final ItemCompanyIdResolver itemCompanyIdResolver;
	private final ItemsRepository itemsRepository;
	private final SearchIndexSyncScheduler searchIndexSyncScheduler;

	public ItemStoreUpdatedSearchIndexDispatchListener(
			ItemCompanyIdResolver itemCompanyIdResolver,
			ItemsRepository itemsRepository,
			SearchIndexSyncScheduler searchIndexSyncScheduler) {
		this.itemCompanyIdResolver = itemCompanyIdResolver;
		this.itemsRepository = itemsRepository;
		this.searchIndexSyncScheduler = searchIndexSyncScheduler;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		try {
			long itemId = toLong(payload.get("item_id"));
			long distributorId = toLong(payload.get("distributor_id"));
			Optional<Long> companyId = itemCompanyIdResolver.findCompanyIdByItemId(itemId);
			if (companyId.isEmpty()) {
				return;
			}
			long defaultItemId = resolveDefaultItemId(companyId.get(), itemId);
			if (defaultItemId <= 0) {
				return;
			}
			searchIndexSyncScheduler.scheduleUpsert(companyId.get(), distributorId, defaultItemId);
		} catch (RuntimeException e) {
			log.warn("item store search index schedule failed payload={}", payload, e);
		}
	}

	private long resolveDefaultItemId(long companyId, long itemId) {
		Items row = itemsRepository.findByItemId(itemId);
		if (row == null || row.getCompanyId() == null || row.getCompanyId() != companyId) {
			return itemId;
		}
		Long def = row.getDefaultItemId();
		if (def != null && def > 0) {
			return def;
		}
		return itemId;
	}

	private static long toLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
