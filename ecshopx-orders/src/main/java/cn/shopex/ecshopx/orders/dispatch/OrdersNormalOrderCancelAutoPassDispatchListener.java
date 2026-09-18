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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderPassRefundService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrdersNormalOrderCancelAutoPassDispatchListener implements DispatchListener {

	private static final Logger log = LoggerFactory.getLogger(OrdersNormalOrderCancelAutoPassDispatchListener.class);

	private final AdminOrderPassRefundService adminOrderPassRefundService;
	private final AftersalesRefundService aftersalesRefundService;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final NormalOrdersMapper normalOrdersMapper;

	public OrdersNormalOrderCancelAutoPassDispatchListener(
			AdminOrderPassRefundService adminOrderPassRefundService,
			AftersalesRefundService aftersalesRefundService,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			NormalOrdersMapper normalOrdersMapper) {
		this.adminOrderPassRefundService = adminOrderPassRefundService;
		this.aftersalesRefundService = aftersalesRefundService;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Long companyId = longObject(payload.get("company_id"));
		Long orderId = longObject(payload.get("order_id"));
		if (companyId == null || orderId == null) {
			return;
		}
		Map<String, Object> platform = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		if (!Boolean.TRUE.equals(platform.get("auto_aftersales"))) {
			return;
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			return;
		}
		if (!"PAYED".equalsIgnoreCase(safe(order.getOrderStatus()))) {
			return;
		}
		if (!"WAIT_PROCESS".equalsIgnoreCase(safe(order.getCancelStatus()))) {
			return;
		}
		while (true) {
			List<AftersalesRefund> ready =
					aftersalesRefundService.listReadyCancelRefundsAsc(companyId, orderId, 0L);
			if (ready == null || ready.isEmpty()) {
				break;
			}
			AftersalesRefund refund = ready.get(0);
			if (!"READY".equalsIgnoreCase(safe(refund.getRefundStatus()))) {
				break;
			}
			long refundSupplierId = refund.getSupplierId() == null ? 0L : refund.getSupplierId();
			Map<String, Object> refundFilter = new LinkedHashMap<>();
			refundFilter.put("company_id", companyId);
			refundFilter.put("order_id", orderId);
			refundFilter.put("supplier_id", refundSupplierId);
			refundFilter.put("refund_bn", refund.getRefundBn());
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("company_id", companyId);
			params.put("order_id", orderId);
			params.put("supplier_id", refundSupplierId);
			params.put("check_cancel", "1");
			String orderType = safe(order.getOrderType());
			params.put("order_type", StringUtils.hasText(orderType) ? orderType : "normal");
			params.put("operator_type", "system");
			params.put("operator_id", 0L);
			try {
				adminOrderPassRefundService.passRefund(refundFilter, refund, params);
			} catch (RuntimeException ex) {
				log.warn(
						"normal_order_cancel_auto_pass failed companyId={} orderId={} refundBn={} reason={}",
						companyId,
						orderId,
						refund.getRefundBn(),
						ex.getMessage());
				break;
			}
		}
	}

	private static Long longObject(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
