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

package cn.shopex.ecshopx.orders.service.tradeexport.support;

import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.service.admin.support.TradeTimeStartColumnConditions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.util.StringUtils;

public final class TradeExportQuerySupport {

	private TradeExportQuerySupport() {}

	public static LambdaQueryWrapper<Trade> toCountWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<Trade> w = new LambdaQueryWrapper<>();
		applyFilter(companyId, w, filter);
		return w;
	}

	public static LambdaQueryWrapper<Trade> toListWrapper(long companyId, LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<Trade> w = new LambdaQueryWrapper<>();
		applyFilter(companyId, w, filter);
		boolean orderByExpire = StringUtils.hasText(str(filter.get("order_id")));
		if (orderByExpire) {
			w.orderByDesc(Trade::getTimeExpire).orderByDesc(Trade::getTimeStart);
		} else {
			w.orderByDesc(Trade::getTimeStart);
		}
		return w;
	}

	private static void applyFilter(long companyId, LambdaQueryWrapper<Trade> w, LinkedHashMap<String, Object> filter) {
		w.eq(Trade::getCompanyId, String.valueOf(companyId));
		Object merchantIdObj = filter.get("merchant_id");
		if (merchantIdObj instanceof Number n && n.longValue() > 0L) {
			w.eq(Trade::getMerchantId, n.longValue());
		}
		String tradeState = str(filter.get("trade_state"));
		if (StringUtils.hasText(tradeState)) {
			w.eq(Trade::getTradeState, tradeState);
		}
		String orderId = str(filter.get("order_id"));
		if (StringUtils.hasText(orderId)) {
			w.eq(Trade::getOrderId, orderId);
		}
		String tsBegin = str(filter.get("time_start_begin"));
		String tsEnd = str(filter.get("time_start_end"));
		TradeTimeStartColumnConditions.apply(w, tsBegin, tsEnd);
		String shopSingle = str(filter.get("shop_id"));
		if (StringUtils.hasText(shopSingle)) {
			w.eq(Trade::getShopId, shopSingle);
		} else {
			Object shopInObj = filter.get("shop_id_in");
			if (shopInObj instanceof List<?> list && !list.isEmpty()) {
				List<String> ids =
						list.stream()
								.map(o -> o == null ? "" : String.valueOf(o).trim())
								.filter(StringUtils::hasText)
								.toList();
				if (!ids.isEmpty()) {
					w.in(Trade::getShopId, ids);
				}
			}
		}
		Object distEqObj = filter.get("distributor_eq");
		if (distEqObj instanceof Number n && n.longValue() > 0L) {
			w.eq(Trade::getDistributorId, String.valueOf(n.longValue()));
		} else {
			Object distInObj = filter.get("distributor_id_in");
			if (distInObj instanceof List<?> list && !list.isEmpty()) {
				List<String> ids =
						list.stream()
								.map(o -> o == null ? "" : String.valueOf(o).trim())
								.filter(StringUtils::hasText)
								.toList();
				if (!ids.isEmpty()) {
					w.in(Trade::getDistributorId, ids);
				}
			}
		}
		Object tradeSourceInObj = filter.get("trade_source_in");
		if (tradeSourceInObj instanceof List<?> list && !list.isEmpty()) {
			List<String> srcs =
					list.stream()
							.map(o -> o == null ? "" : String.valueOf(o).trim())
							.filter(StringUtils::hasText)
							.toList();
			if (!srcs.isEmpty()) {
				w.in(Trade::getTradeSourceType, srcs);
			}
		} else {
			String tradeSourceEq = str(filter.get("trade_source_eq"));
			if (StringUtils.hasText(tradeSourceEq)) {
				w.eq(Trade::getTradeSourceType, tradeSourceEq);
			}
		}
		String userId = str(filter.get("user_id"));
		if (StringUtils.hasText(userId)) {
			w.eq(Trade::getUserId, userId);
		} else if (StringUtils.hasText(str(filter.get("mobile_cipher")))) {
			w.eq(Trade::getMobile, str(filter.get("mobile_cipher")));
		} else if (StringUtils.hasText(str(filter.get("trade_id_exact")))) {
			w.eq(Trade::getTradeId, str(filter.get("trade_id_exact")));
		}
	}

	private static String str(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
