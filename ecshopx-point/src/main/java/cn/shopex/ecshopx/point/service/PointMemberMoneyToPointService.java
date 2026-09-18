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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PointMemberMoneyToPointService {

	private final PointMemberRuleReadService pointMemberRuleReadService;

	public PointMemberMoneyToPointService(PointMemberRuleReadService pointMemberRuleReadService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
	}

	public long moneyToPoint(long companyId, long moneyFen) {
		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
		Object openFlag = rule.get("isOpenMemberPoint");
		boolean enabled = "true".equals(String.valueOf(openFlag));
		if (!enabled) {
			Object nameObj = rule.get("name");
			String name = nameObj == null || nameObj.toString().isEmpty() ? "积分" : nameObj.toString();
			throw new ResourceException(name + "支付未开启");
		}
		Object deductObj = rule.get("deduct_point");
		BigDecimal deductPoint =
				deductObj == null
						? BigDecimal.ZERO
						: new BigDecimal(deductObj.toString().trim());
		BigDecimal yuan =
				BigDecimal.valueOf(moneyFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
		BigDecimal scaled = yuan.multiply(deductPoint).setScale(2, RoundingMode.DOWN);
		return scaled.setScale(0, RoundingMode.CEILING).longValue();
	}
}
