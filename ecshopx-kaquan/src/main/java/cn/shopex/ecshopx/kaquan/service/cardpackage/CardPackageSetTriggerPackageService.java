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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CardPackageSetTriggerPackageService {

	private final CardPackageReceiveRecordDedupService dedupService;
	private final CardPackageTriggerPackageIdsService triggerPackageIdsService;
	private final CardPackageReceivesPackageService receivesPackageService;

	public CardPackageSetTriggerPackageService(CardPackageReceiveRecordDedupService dedupService,
			CardPackageTriggerPackageIdsService triggerPackageIdsService,
			CardPackageReceivesPackageService receivesPackageService) {
		this.dedupService = dedupService;
		this.triggerPackageIdsService = triggerPackageIdsService;
		this.receivesPackageService = receivesPackageService;
	}

	public void triggerPackage(long companyId, long userId, long gradeId, String triggerType, boolean reissue) {
		if (!reissue) {
			if (dedupService.find(companyId, userId, gradeId, triggerType).isPresent()) {
				return;
			}
		}
		List<Long> packageIdList = triggerPackageIdsService.listPackageIds(companyId, gradeId, triggerType);
		if (packageIdList.isEmpty()) {
			return;
		}
		for (Long packageId : packageIdList) {
			try {
				receivesPackageService.receivesPackage(companyId, packageId, userId, triggerType, 0L);
			} catch (Exception e) {
				log.debug("卡券包发放失败: {}", e.getMessage(), e);
			}
		}
		dedupService.insert(companyId, userId, gradeId, triggerType);
	}
}
