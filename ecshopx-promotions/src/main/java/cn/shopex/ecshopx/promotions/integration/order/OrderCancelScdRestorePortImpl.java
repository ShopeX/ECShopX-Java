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

package cn.shopex.ecshopx.promotions.integration.order;

import cn.shopex.ecshopx.common.port.order.OrderCancelScdRestorePort;
import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscountRelUser;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountRelUserMapper;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;

/**
 * 订单取消后回退定向促销折扣（INSERT promotions_scd_rel_user action_type='less'）。
 */
@Service
public class OrderCancelScdRestorePortImpl implements OrderCancelScdRestorePort {

	private final SpecificCrowdDiscountRelUserMapper specificCrowdDiscountRelUserMapper;

	public OrderCancelScdRestorePortImpl(
			SpecificCrowdDiscountRelUserMapper specificCrowdDiscountRelUserMapper) {
		this.specificCrowdDiscountRelUserMapper = specificCrowdDiscountRelUserMapper;
	}

	@Override
	public void lessDiscount(long companyId, long userId, Map<String, Object> discountEntry) {
		if (discountEntry == null || discountEntry.isEmpty()) {
			return;
		}
		Long activityId = longVal(discountEntry.get("activity_id"));
		Long specificId = longVal(discountEntry.get("specific_id"));
		Long discountFee = longVal(discountEntry.get("discount_fee"));
		if (activityId == null || activityId <= 0L) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		String ym = YearMonth.now(ZoneId.systemDefault()).toString().replace("-", "");
		SpecificCrowdDiscountRelUser row = new SpecificCrowdDiscountRelUser();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setOrderId("");
		row.setDiscountFee(discountFee != null ? discountFee : 0L);
		row.setActivityId(activityId);
		row.setSpecificId(specificId);
		row.setActivityMonth(ym);
		row.setActionType("less");
		row.setCreated(now);
		row.setUpdated(now);
		specificCrowdDiscountRelUserMapper.insert(row);
	}

	private static Long longVal(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
