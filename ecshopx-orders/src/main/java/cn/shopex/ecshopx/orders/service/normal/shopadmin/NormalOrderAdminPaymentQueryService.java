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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentTradeQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderAdminPaymentQueryService {

	private final OrdersPaymentTradeQueryService ordersPaymentTradeQueryService;
	private final ShopadminDeferredOrderService shopadminDeferredOrderService;
	private final TradeMapper tradeMapper;

	public NormalOrderAdminPaymentQueryService(
			OrdersPaymentTradeQueryService ordersPaymentTradeQueryService,
			ShopadminDeferredOrderService shopadminDeferredOrderService,
			TradeMapper tradeMapper) {
		this.ordersPaymentTradeQueryService = ordersPaymentTradeQueryService;
		this.shopadminDeferredOrderService = shopadminDeferredOrderService;
		this.tradeMapper = tradeMapper;
	}

	public Map<String, Object> queryPayment(HttpServletRequest request, String tradeId) {
		Map<String, Object> authInfo = readAuthInfoMap(request);
		Map<String, Object> data = ordersPaymentTradeQueryService.resolve(tradeId, authInfo);
		if (StringUtils.hasText(tradeId)) {
			Trade trade = tradeMapper.selectById(tradeId);
			long companyId = longVal(authInfo.get("company_id"), 0L);
			if (companyId > 0L) {
				shopadminDeferredOrderService.supplementAfterPaymentQuery(companyId, authInfo, trade);
			}
		}
		return data;
	}

	private static Map<String, Object> readAuthInfoMap(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) attr;
		return map;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
