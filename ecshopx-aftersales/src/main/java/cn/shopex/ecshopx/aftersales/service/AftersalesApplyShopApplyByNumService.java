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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Shop apply-by-quantity for merchant API v1 apply; handle layer may fan out async trade-refund dispatch after commit. */
@Service
public class AftersalesApplyShopApplyByNumService {

	private final AftersalesApplyCheckApplyService aftersalesApplyCheckApplyService;
	private final AftersalesApplyShopApplyByNumHandleService aftersalesApplyShopApplyByNumHandleService;
	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderSuccessTradeReadPort orderSuccessTradeReadPort;

	public AftersalesApplyShopApplyByNumService(
			AftersalesApplyCheckApplyService aftersalesApplyCheckApplyService,
			AftersalesApplyShopApplyByNumHandleService aftersalesApplyShopApplyByNumHandleService,
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderSuccessTradeReadPort orderSuccessTradeReadPort) {
		this.aftersalesApplyCheckApplyService = aftersalesApplyCheckApplyService;
		this.aftersalesApplyShopApplyByNumHandleService = aftersalesApplyShopApplyByNumHandleService;
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderSuccessTradeReadPort = orderSuccessTradeReadPort;
	}

	public void shopApplyByNum(AftersalesApplyParams params) {
		aftersalesApplyCheckApplyService.checkApply(params);
		long companyId = params.getCompanyId();
		long orderId = params.getOrderId();

		Optional<Map<String, Object>> headOpt = orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (headOpt.isEmpty()) {
			throw new ResourceException("系统无此订单，无法申请售后");
		}
		Map<String, Object> orderInfo = new LinkedHashMap<>(headOpt.get());

		Optional<Map<String, Object>> tradeOpt =
				orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId);
		if (tradeOpt.isEmpty()) {
			throw new ResourceException("无有效支付交易单，无法申请售后");
		}
		Map<String, Object> trade = tradeOpt.get();

		List<Map<String, Object>> detailRows = params.getDetailRows();
		if (detailRows == null) {
			return;
		}
		for (Map<String, Object> v : detailRows) {
			Map<String, Object> detailTmp = new LinkedHashMap<>(v);
			int originalLineNum = intVal(detailTmp.get("num"));
			for (int i = 0; i < originalLineNum; i++) {
				if (detailTmp.get("total_fee") != null && intVal(detailTmp.get("total_fee")) > 0) {
					int tf = intVal(detailTmp.get("total_fee"));
					int dn = originalLineNum;
					if (dn > 0) {
						int next =
								new BigDecimal(tf)
										.multiply(new BigDecimal("100"))
										.divide(new BigDecimal(dn), 2, RoundingMode.HALF_UP)
										.setScale(0, RoundingMode.FLOOR)
										.intValue();
						detailTmp.put("total_fee", next);
					}
				}
				if (detailTmp.get("total_point") != null && intVal(detailTmp.get("total_point")) > 0) {
					int tp = intVal(detailTmp.get("total_point"));
					int dn = originalLineNum;
					if (dn > 0) {
						int next =
								new BigDecimal(tp)
										.multiply(new BigDecimal("100"))
										.divide(new BigDecimal(dn), 2, RoundingMode.HALF_UP)
										.setScale(0, RoundingMode.FLOOR)
										.intValue();
						detailTmp.put("total_point", next);
					}
				}
				Map<String, Object> oneRow = new LinkedHashMap<>(detailTmp);
				oneRow.put("num", 1);
				List<Map<String, Object>> single = new ArrayList<>();
				single.add(oneRow);
				Map<String, Object> data = params.toHandleDataMap();
				data.put("detail", single);
				aftersalesApplyShopApplyByNumHandleService.shopApplyByNumHandle(orderInfo, trade, data);
			}
		}
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
