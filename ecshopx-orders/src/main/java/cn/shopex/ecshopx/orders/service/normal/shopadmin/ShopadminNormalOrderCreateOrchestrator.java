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

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService;

@Service
public class ShopadminNormalOrderCreateOrchestrator {

	private final OrderCreateNeedParamsPort orderCreateNeedParamsPort;
	private final OrderCheckoutCartPort orderCheckoutCartPort;
	private final OrderCreateItemCheckPort orderCreateItemCheckPort;
	private final OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;
	private final OrderCreateFormatDataPort orderCreateFormatDataPort;
	private final ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner;
	private final NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher;
	private final SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService;

	public ShopadminNormalOrderCreateOrchestrator(
			OrderCreateNeedParamsPort orderCreateNeedParamsPort,
			OrderCheckoutCartPort orderCheckoutCartPort,
			OrderCreateItemCheckPort orderCreateItemCheckPort,
			OrderCreateDistributorCheckPort orderCreateDistributorCheckPort,
			OrderCreateFormatDataPort orderCreateFormatDataPort,
			ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner,
			NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher,
			SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService) {
		this.orderCreateNeedParamsPort = orderCreateNeedParamsPort;
		this.orderCheckoutCartPort = orderCheckoutCartPort;
		this.orderCreateItemCheckPort = orderCreateItemCheckPort;
		this.orderCreateDistributorCheckPort = orderCreateDistributorCheckPort;
		this.orderCreateFormatDataPort = orderCreateFormatDataPort;
		this.shopadminNormalOrderCreateTransactionalRunner = shopadminNormalOrderCreateTransactionalRunner;
		this.normalOrderAddDispatchPublisher = normalOrderAddDispatchPublisher;
		this.supplierOrderSplitOnNormalOrderAddService = supplierOrderSplitOnNormalOrderAddService;
	}

	public Map<String, Object> create(NormalOrderCreateState state, HttpServletRequest request) {
		assertKnownOrderType(state);
		orderCreateNeedParamsPort.validate(state);
		orderCheckoutCartPort.fillItemsFromOperatorCart(state, request);
		orderCreateItemCheckPort.check(state);
		orderCreateDistributorCheckPort.check(state);
		orderCreateFormatDataPort.format(state);
		shopadminNormalOrderCreateTransactionalRunner.runInTransaction(state, request);
		splitSupplierOrders(state);
		normalOrderAddDispatchPublisher.publish(buildNormalOrderAddPayload(state));
		return buildSuccessPayload(state);
	}

	private static void assertKnownOrderType(NormalOrderCreateState state) {
		String ot = String.valueOf(state.getParams().getOrDefault("order_type", ""));
		if (!"normal_shopadmin".equals(ot)) {
			throw new ResourceException("无此类型订单！");
		}
	}

	private void splitSupplierOrders(NormalOrderCreateState state) {
		Map<String, Object> insert = state.getOrdersInsertResult();
		Long companyId = longObject(insert.get("company_id"));
		Long orderId = longObject(insert.get("order_id"));
		if (companyId == null || orderId == null) {
			return;
		}
		supplierOrderSplitOnNormalOrderAddService.split(companyId, orderId);
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

	private static Map<String, Object> buildNormalOrderAddPayload(NormalOrderCreateState state) {
		Map<String, Object> insert = state.getOrdersInsertResult();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", insert.get("company_id"));
		payload.put("order_id", insert.get("order_id"));
		Object payType = insert.get("pay_type");
		if (payType == null && state.getOrderData() != null) {
			payType = state.getOrderData().get("pay_type");
		}
		payload.put("pay_type", payType);
		return payload;
	}

	private static Map<String, Object> buildSuccessPayload(NormalOrderCreateState state) {
		Map<String, Object> res = new LinkedHashMap<>(state.getOrdersInsertResult());
		Map<String, Object> od = state.getOrderData();
		Map<String, Object> pr = state.getParams();
		if (od != null) {
			copyIfPresent(res, od, "discount_fee");
			copyIfPresent(res, od, "discount_info");
			copyIfPresent(res, od, "items");
			copyIfPresent(res, od, "item_fee");
			copyIfPresent(res, od, "market_fee");
			copyIfPresent(res, od, "auto_cancel_time");
			copyIfPresent(res, od, "mobile");
			copyIfPresent(res, od, "title");
			copyIfPresent(res, od, "order_class");
			copyIfPresent(res, od, "order_type");
			copyIfPresent(res, od, "total_fee");
			copyIfPresent(res, od, "pay_type");
			copyIfPresent(res, od, "receipt_type");
			copyIfPresent(res, od, "point_fee");
			copyIfPresent(res, od, "point_use");
			copyIfPresent(res, od, "operator_id");
			copyIfPresent(res, od, "salesman_id");
			copyIfPresent(res, od, "distributor_id");
			copyIfPresent(res, od, "wxa_appid");
			copyIfPresent(res, od, "authorizer_appid");
		}
		if (pr != null) {
			copyIfPresent(res, pr, "order_source");
			copyIfPresent(res, pr, "promotion");
			copyIfPresent(res, pr, "source_from");
		}
		return res;
	}

	private static void copyIfPresent(Map<String, Object> dest, Map<String, Object> src, String key) {
		if (src.containsKey(key)) {
			dest.put(key, src.get(key));
		}
	}
}
