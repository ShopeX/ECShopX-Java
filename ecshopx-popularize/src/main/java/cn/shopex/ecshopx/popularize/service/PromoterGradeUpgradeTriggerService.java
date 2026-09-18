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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.dispatch.UpgradePromoterGradeJobDispatchPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PromoterGradeUpgradeTriggerService {

	private static final Logger log = LoggerFactory.getLogger(PromoterGradeUpgradeTriggerService.class);

	private final UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher;

	public PromoterGradeUpgradeTriggerService(
			UpgradePromoterGradeJobDispatchPublisher upgradePromoterGradeJobDispatchPublisher) {
		this.upgradePromoterGradeJobDispatchPublisher = upgradePromoterGradeJobDispatchPublisher;
	}

	public void upgradeGrade(long companyId, long userId) {
		if (userId <= 0L) {
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug("enqueue upgrade promoter grade companyId={} userId={}", companyId, userId);
		}
		upgradePromoterGradeJobDispatchPublisher.enqueueUpgradePromoterGrade(companyId, userId);
	}
}
