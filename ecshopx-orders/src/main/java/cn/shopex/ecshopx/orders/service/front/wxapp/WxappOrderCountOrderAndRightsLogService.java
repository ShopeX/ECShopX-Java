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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.domain.RightsLog;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.RightsLogMapper;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.mapper.WxappKaquanUserDiscountCountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderCountOrderAndRightsLogService {

	private final OrderAssociationsMapper orderAssociationsMapper;

	private final RightsMapper rightsMapper;

	private final RightsLogMapper rightsLogMapper;

	private final WxappKaquanUserDiscountCountMapper wxappKaquanUserDiscountCountMapper;

	public WxappOrderCountOrderAndRightsLogService(
			OrderAssociationsMapper orderAssociationsMapper,
			RightsMapper rightsMapper,
			RightsLogMapper rightsLogMapper,
			WxappKaquanUserDiscountCountMapper wxappKaquanUserDiscountCountMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.rightsMapper = rightsMapper;
		this.rightsLogMapper = rightsLogMapper;
		this.wxappKaquanUserDiscountCountMapper = wxappKaquanUserDiscountCountMapper;
	}

	public Map<String, Object> countOrderAndRightsLog(
			Map<String, Object> auth, String orderTypeRaw, boolean orderTypeKeyPresent) {
		Object uidObj = auth.get("user_id");
		if (isUserIdFalsy(uidObj)) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("rightsLogTotal", 0L);
			out.put("rightsTotal", 0L);
			out.put("orderTotal", 0L);
			out.put("couponTotal", 0L);
			return out;
		}

		long userId = longVal(uidObj);
		long companyId = longVal(auth.get("company_id"));

		String orderType;
		if (!orderTypeKeyPresent) {
			orderType = "service";
		} else {
			orderType = orderTypeRaw == null ? "" : orderTypeRaw.trim();
		}

		long rightsLogTotal = 0L;
		long rightsTotal = 0L;
		if ("service".equals(orderType)) {
			LambdaQueryWrapper<RightsLog> logW = new LambdaQueryWrapper<RightsLog>()
					.eq(RightsLog::getUserId, userId)
					.eq(RightsLog::getCompanyId, companyId);
			Long rl = rightsLogMapper.selectCount(logW);
			rightsLogTotal = rl == null ? 0L : rl.longValue();

			LambdaQueryWrapper<Rights> rW = new LambdaQueryWrapper<Rights>()
					.eq(Rights::getUserId, userId)
					.eq(Rights::getCompanyId, companyId);
			Long rt = rightsMapper.selectCount(rW);
			rightsTotal = rt == null ? 0L : rt.longValue();
		}

		LambdaQueryWrapper<OrderAssociations> oW = new LambdaQueryWrapper<OrderAssociations>()
				.eq(OrderAssociations::getUserId, userId)
				.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderStatus, "DONE");
		boolean orderTypeFilterPresent =
				orderType != null && !orderType.isBlank() && !"0".equals(orderType);
		if (orderTypeFilterPresent) {
			oW.eq(OrderAssociations::getOrderType, orderType);
		}
		Long ot = orderAssociationsMapper.selectCount(oW);
		long orderTotal = ot == null ? 0L : ot.longValue();

		int nowEpoch = (int) (System.currentTimeMillis() / 1000L);
		Long couponRaw = wxappKaquanUserDiscountCountMapper.countValidForWxappOrderStats(companyId, userId, nowEpoch);
		long couponTotal = couponRaw == null ? 0L : couponRaw.longValue();

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if ("service".equals(orderType)) {
			out.put("rightsLogTotal", rightsLogTotal);
			out.put("rightsTotal", rightsTotal);
		}
		out.put("orderTotal", orderTotal);
		out.put("couponTotal", couponTotal);
		return out;
	}

	private static boolean isUserIdFalsy(Object uidObj) {
		if (uidObj == null) {
			return true;
		}
		if (uidObj instanceof Boolean b) {
			return !b;
		}
		String s = uidObj.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return true;
		}
		return longVal(uidObj) <= 0L;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
