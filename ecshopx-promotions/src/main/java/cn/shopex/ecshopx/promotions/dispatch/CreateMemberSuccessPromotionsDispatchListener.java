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

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.service.CreateMemberSuccessRegisterPromotionExecutionService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreateMemberSuccessPromotionsDispatchListener implements DispatchListener {

	private final CreateMemberSuccessRegisterPromotionExecutionService executionService;

	public CreateMemberSuccessPromotionsDispatchListener(
			CreateMemberSuccessRegisterPromotionExecutionService executionService) {
		this.executionService = executionService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (!Boolean.TRUE.equals(payload.get("if_register_promotion"))) {
			return;
		}
		Object rawCompany = payload.get("company_id");
		Object rawDist = payload.get("distributor_id");
		Object rawUser = payload.get("user_id");
		Object rawMobile = payload.get("mobile");
		if (rawCompany == null || rawDist == null || rawUser == null || rawMobile == null) {
			throw new BadRequestException("company_id, distributor_id, user_id and mobile are required");
		}
		String mobile =
				rawMobile instanceof String s ? s.trim() : String.valueOf(rawMobile).trim();
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("mobile is required");
		}
		long companyId = ((Number) rawCompany).longValue();
		long distributorId = ((Number) rawDist).longValue();
		long userId = ((Number) rawUser).longValue();
		executionService.executionMarketingAfterMemberCreate(companyId, distributorId, userId, mobile);
	}
}
