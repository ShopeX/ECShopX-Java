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

package cn.shopex.ecshopx.promotions.service.event;

import cn.shopex.ecshopx.promotions.event.SeckillActivityEndCommittedEvent;
import cn.shopex.ecshopx.promotions.service.MarketingActivityPostCommitJobsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SeckillActivityEndCommittedEventListener {

	private final MarketingActivityPostCommitJobsService marketingActivityPostCommitJobsService;

	public SeckillActivityEndCommittedEventListener(
			MarketingActivityPostCommitJobsService marketingActivityPostCommitJobsService) {
		this.marketingActivityPostCommitJobsService = marketingActivityPostCommitJobsService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onEndCommitted(SeckillActivityEndCommittedEvent event) {
		marketingActivityPostCommitJobsService.enqueueSeckillEndSalespersonItemsShelves(
				event.companyId(), event.seckillId(), event.seckillTypeRaw());
	}
}
