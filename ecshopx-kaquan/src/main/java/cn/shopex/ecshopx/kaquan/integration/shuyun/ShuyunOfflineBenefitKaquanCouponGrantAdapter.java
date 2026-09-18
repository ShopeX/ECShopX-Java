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

package cn.shopex.ecshopx.kaquan.integration.shuyun;

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOfflineBenefitCouponGrantPort;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 真实发券：委托 {@link UserDiscountReceiveCardService#receiveCard}。
 * 对齐 PHP {@code ShuyunOfflineBenefitKaquanCouponGrantAdapter}。
 */
@Service
@Primary
public class ShuyunOfflineBenefitKaquanCouponGrantAdapter implements ShuyunOfflineBenefitCouponGrantPort {

	private final UserDiscountReceiveCardService userDiscountReceiveCardService;

	public ShuyunOfflineBenefitKaquanCouponGrantAdapter(
			UserDiscountReceiveCardService userDiscountReceiveCardService) {
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
	}

	@Override
	public Map<String, Object> grantByCardTemplate(long companyId, long cardId, long userId, String sourceFrom) {
		return userDiscountReceiveCardService.receiveCard(
				companyId, userId, "", cardId, 0L, "", sourceFrom);
	}
}
