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
		doShopApplyByNum(params);
	}

	public void shopApplyByNum(
			AftersalesApplyParams params, boolean shareFullDeliveredAftersalesQuantity) {
		aftersalesApplyCheckApplyService.checkApply(params, shareFullDeliveredAftersalesQuantity);
		doShopApplyByNum(params);
	}

	private void doShopApplyByNum(AftersalesApplyParams params) {
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
			int originalLineNum = intVal(v.get("num"));
			for (int i = 0; i < originalLineNum; i++) {
				Map<String, Object> oneRow = new LinkedHashMap<>(v);
				oneRow.remove("total_fee");
				oneRow.remove("total_point");
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
