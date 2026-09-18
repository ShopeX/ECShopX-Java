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

package cn.shopex.ecshopx.popularize.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.popularize.service.UpgradePromoterGradeJobRunService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UpgradePromoterGradeJobHandler implements DispatchHandler {

	private final UpgradePromoterGradeJobRunService upgradePromoterGradeJobRunService;

	public UpgradePromoterGradeJobHandler(UpgradePromoterGradeJobRunService upgradePromoterGradeJobRunService) {
		this.upgradePromoterGradeJobRunService = upgradePromoterGradeJobRunService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		long userId = readLong(payload.get("user_id"), 0L);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		upgradePromoterGradeJobRunService.run(companyId, userId);
	}

	private static long readLong(Object raw, long defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
