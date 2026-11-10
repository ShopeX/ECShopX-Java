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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 将增量索引更新推迟到事务提交后（无活跃事务则立即发布）。 */
@Component
public class SearchIndexSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(SearchIndexSyncScheduler.class);

	private final ApplicationEventPublisher eventPublisher;

	public SearchIndexSyncScheduler(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void scheduleUpsert(long companyId, long distributorId, long defaultItemId) {
		schedule(new GoodsSearchDirtyEvent(companyId, distributorId, defaultItemId, GoodsSearchDirtyEvent.Op.UPSERT));
	}

	public void scheduleDelete(long companyId, long distributorId, long defaultItemId) {
		schedule(new GoodsSearchDirtyEvent(companyId, distributorId, defaultItemId, GoodsSearchDirtyEvent.Op.DELETE));
	}

	private void schedule(GoodsSearchDirtyEvent event) {
		if (TransactionSynchronizationManager.isActualTransactionActive()
				&& TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publishSafely(event);
				}
			});
			return;
		}
		publishSafely(event);
	}

	private void publishSafely(GoodsSearchDirtyEvent event) {
		try {
			eventPublisher.publishEvent(event);
		} catch (RuntimeException e) {
			log.warn(
					"failed to publish goods search dirty event companyId={} distributorId={} defaultItemId={}",
					event.companyId(),
					event.distributorId(),
					event.defaultItemId(),
					e);
		}
	}
}
