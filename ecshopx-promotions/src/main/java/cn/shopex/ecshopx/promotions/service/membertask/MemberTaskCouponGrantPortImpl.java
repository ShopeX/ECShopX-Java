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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberTaskCouponGrantPortImpl implements MemberTaskCouponGrantPort {

	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final MemberAccountService memberAccountService;

	public MemberTaskCouponGrantPortImpl(
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			MemberAccountService memberAccountService) {
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.memberAccountService = memberAccountService;
	}

	@Override
	public void grantCoupon(long companyId, long userId, long couponId) {
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		String mobile = member == null || member.get("mobile") == null ? "" : String.valueOf(member.get("mobile"));
		if (!StringUtils.hasText(mobile)) {
			throw new IllegalStateException("会员手机号为空，无法发券");
		}
		userDiscountReceiveCardService.receiveCard(
				companyId, userId, mobile, couponId, 0L, "", "会员任务奖励");
	}
}
