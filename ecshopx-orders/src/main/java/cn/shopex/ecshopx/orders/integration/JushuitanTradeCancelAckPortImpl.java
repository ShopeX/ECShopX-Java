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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.JushuitanTradeCancelAckPort;
import cn.shopex.ecshopx.orders.domain.OrdersRelJushuitan;
import cn.shopex.ecshopx.orders.mapper.OrdersRelJushuitanMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderPassRefundService;
import cn.shopex.ecshopx.orders.service.admin.OrdersConfirmCancelRefundStatuses;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class JushuitanTradeCancelAckPortImpl implements JushuitanTradeCancelAckPort {

	private static final Logger log = LoggerFactory.getLogger(JushuitanTradeCancelAckPortImpl.class);

	private final OrdersRelJushuitanMapper ordersRelJushuitanMapper;
	private final AftersalesRefundService aftersalesRefundService;
	private final ApplicationContext applicationContext;

	public JushuitanTradeCancelAckPortImpl(
			OrdersRelJushuitanMapper ordersRelJushuitanMapper,
			AftersalesRefundService aftersalesRefundService,
			ApplicationContext applicationContext) {
		this.ordersRelJushuitanMapper = ordersRelJushuitanMapper;
		this.aftersalesRefundService = aftersalesRefundService;
		this.applicationContext = applicationContext;
	}

	@Override
	public void acknowledgeAfterPlatformCancel(long companyId, long orderId, Map<String, Object> tradeCancelPayload) {
		ordersRelJushuitanMapper.delete(
				new LambdaQueryWrapper<OrdersRelJushuitan>()
						.eq(OrdersRelJushuitan::getCompanyId, companyId)
						.eq(OrdersRelJushuitan::getOrderId, orderId));
		if (tradeCancelPayload == null) {
			return;
		}
		Object refundBn = tradeCancelPayload.get("refund_bn");
		List<AftersalesRefund> list =
				aftersalesRefundService.listForPointsmallConfirmCancel(
						companyId,
						orderId,
						refundBn,
						OrdersConfirmCancelRefundStatuses.REFUND_STATUSES_FOR_ABSTRACT_CANCEL_LOOKUP);
		if (list == null || list.isEmpty()) {
			throw new ResourceException("没有查到退款单，无法同意取消订单");
		}
		Map<String, Object> params = buildConfirmParams(companyId, orderId, tradeCancelPayload);
		AdminOrderPassRefundService proxy = applicationContext.getBean(AdminOrderPassRefundService.class);
		boolean anyReady = false;
		for (AftersalesRefund refund : list) {
			String rs = safe(refund.getRefundStatus());
			if (!"READY".equalsIgnoreCase(rs)) {
				continue;
			}
			anyReady = true;
			proxy.transactionalPointsmallOne(refund, "1", "", params);
		}
		if (!anyReady) {
			log.debug(
					"jushuitan trade cancel confirm skipped: no READY refunds companyId={} orderId={}",
					companyId,
					orderId);
		}
	}

	private static Map<String, Object> buildConfirmParams(
			long companyId, long orderId, Map<String, Object> tradeCancelPayload) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		if (tradeCancelPayload != null) {
			params.putAll(tradeCancelPayload);
		}
		params.put("company_id", companyId);
		params.put("order_id", orderId);
		String operatorType = str(params.get("operator_type"));
		long operatorId = longVal(params.get("operator_id"));
		long supplierId = 0L;
		if (Objects.equals("supplier", safeOperatorType(operatorType))) {
			supplierId = operatorId;
		}
		long explicitSupplier = longVal(params.get("supplier_id"));
		if (explicitSupplier > 0L) {
			supplierId = explicitSupplier;
		}
		params.put("supplier_id", supplierId);
		params.put("check_cancel", "1");
		params.put("shop_reject_reason", "");
		String orderType = str(params.get("order_type"));
		params.put("order_type", orderType.isEmpty() ? "normal_pointsmall" : orderType);
		params.put("operator_type", operatorType);
		params.put("operator_id", operatorId);
		return params;
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String safeOperatorType(String operatorType) {
		return operatorType == null ? "" : operatorType.trim();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
