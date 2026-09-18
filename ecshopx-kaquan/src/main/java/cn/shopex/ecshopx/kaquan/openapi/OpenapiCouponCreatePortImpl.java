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

package cn.shopex.ecshopx.kaquan.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiCouponCreatePort;
import cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1.OpenapiThirdApiV1CouponUserGetCardService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiCouponCreatePortImpl implements OpenapiCouponCreatePort {

	private final OpenapiThirdApiV1CouponUserGetCardService userGetCardService;

	public OpenapiCouponCreatePortImpl(OpenapiThirdApiV1CouponUserGetCardService userGetCardService) {
		this.userGetCardService = userGetCardService;
	}

	@Override
	public Map<String, Object> userGetCard(
			long companyId,
			String templateCode,
			String cardRuleCode,
			String userDiscountCode,
			long startTimeEpoch,
			long endTimeEpoch,
			String outerCrmUserid,
			String mobileUserId) {
		return userGetCardService.executeOpenapiUserGetCard(
				companyId,
				templateCode,
				cardRuleCode,
				userDiscountCode,
				startTimeEpoch,
				endTimeEpoch,
				outerCrmUserid,
				mobileUserId);
	}
}
