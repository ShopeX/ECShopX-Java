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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappPromoterPointConvertService {

	private final PointMemberRuleReadService pointMemberRuleReadService;

	public WxappPromoterPointConvertService(PointMemberRuleReadService pointMemberRuleReadService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
	}

	public long moneyToPointSendEquivalent(long companyId, long moneyFen) {
		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
		if (!"true".equals(String.valueOf(rule.get("isOpenDeductPoint")))
				|| !"true".equals(String.valueOf(rule.get("isOpenMemberPoint")))) {
			return 0L;
		}
		Object dp = rule.get("deduct_point");
		if (dp == null) {
			return 0L;
		}
		BigDecimal deductPoint;
		if (dp instanceof Number n) {
			deductPoint = BigDecimal.valueOf(n.doubleValue());
		} else {
			try {
				deductPoint = new BigDecimal(dp.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (deductPoint.compareTo(BigDecimal.ZERO) <= 0) {
			return 0L;
		}
		BigDecimal yuan = BigDecimal.valueOf(moneyFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
		BigDecimal point = yuan.multiply(deductPoint).setScale(2, RoundingMode.DOWN);
		return point.setScale(0, RoundingMode.CEILING).longValue();
	}
}
