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

import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 按 default_item_id 将搜索索引脏标记扇出到各店铺（含总部 distributor_id=0）。 */
@Component
public class GoodsSearchIndexDirtyFanout {

	private static final Logger log = LoggerFactory.getLogger(GoodsSearchIndexDirtyFanout.class);

	private final DistributorItemsRepository distributorItemsRepository;
	private final SearchIndexSyncScheduler searchIndexSyncScheduler;

	public GoodsSearchIndexDirtyFanout(
			DistributorItemsRepository distributorItemsRepository,
			SearchIndexSyncScheduler searchIndexSyncScheduler) {
		this.distributorItemsRepository = distributorItemsRepository;
		this.searchIndexSyncScheduler = searchIndexSyncScheduler;
	}

	public void scheduleUpsertForDefaultItem(long companyId, long defaultItemId) {
		if (companyId <= 0 || defaultItemId <= 0) {
			return;
		}
		for (long distributorId : distributorIdsForDefaultItem(companyId, defaultItemId)) {
			scheduleUpsertSafely(companyId, distributorId, defaultItemId);
		}
	}

	public void scheduleDeleteForDefaultItem(long companyId, long defaultItemId) {
		if (companyId <= 0 || defaultItemId <= 0) {
			return;
		}
		for (long distributorId : distributorIdsForDefaultItem(companyId, defaultItemId)) {
			scheduleDeleteSafely(companyId, distributorId, defaultItemId);
		}
	}

	private Set<Long> distributorIdsForDefaultItem(long companyId, long defaultItemId) {
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		out.add(0L);
		out.addAll(distributorItemsRepository.listDistinctDistributorIdsByCompanyAndDefaultItemId(companyId, defaultItemId));
		return out;
	}

	private void scheduleUpsertSafely(long companyId, long distributorId, long defaultItemId) {
		try {
			searchIndexSyncScheduler.scheduleUpsert(companyId, distributorId, defaultItemId);
		} catch (RuntimeException e) {
			log.warn(
					"schedule search index upsert failed companyId={} distributorId={} defaultItemId={}",
					companyId,
					distributorId,
					defaultItemId,
					e);
		}
	}

	private void scheduleDeleteSafely(long companyId, long distributorId, long defaultItemId) {
		try {
			searchIndexSyncScheduler.scheduleDelete(companyId, distributorId, defaultItemId);
		} catch (RuntimeException e) {
			log.warn(
					"schedule search index delete failed companyId={} distributorId={} defaultItemId={}",
					companyId,
					distributorId,
					defaultItemId,
					e);
		}
	}
}
