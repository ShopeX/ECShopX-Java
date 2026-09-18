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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PromotionGroupActivityCommittedEventListener {

	private static final Logger log = LoggerFactory.getLogger(PromotionGroupActivityCommittedEventListener.class);

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCommitted(PromotionGroupActivityCommittedEvent event) {
		log.info(
				"post-commit group activity: companyId={} groupsActivityId={} (SavePromotionItemTag placeholder)",
				event.getCompanyId(),
				event.getGroupsActivityId());
		log.info(
				"post-commit group activity: companyId={} groupsActivityId={} (SalespersonItemsShelvesJob placeholder)",
				event.getCompanyId(),
				event.getGroupsActivityId());
	}
}
