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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.promotions.service.PromotionActivityFireService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirePromotionsActivityJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(FirePromotionsActivityJobHandler.class);

	private final PromotionActivityFireService promotionActivityFireService;

	public FirePromotionsActivityJobHandler(PromotionActivityFireService promotionActivityFireService) {
		this.promotionActivityFireService = promotionActivityFireService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			long companyId = extractLong(payload.get("company_id"));
			@SuppressWarnings("unchecked")
			Map<String, Object> memberInfo = (Map<String, Object>) payload.get("member_info");
			String activityType =
					payload.get("activity_type") == null ? "" : String.valueOf(payload.get("activity_type"));
			if (memberInfo == null) {
				memberInfo = Map.of();
			}
			promotionActivityFireService.fire(companyId, memberInfo, activityType);
		} catch (RuntimeException e) {
			log.debug("FirePromotionsActivityJob failed: {}", e.toString());
		}
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
