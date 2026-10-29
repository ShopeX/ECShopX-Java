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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopadminNormalOrderCreateOrchestrator {

	private final OrderCreateNeedParamsPort orderCreateNeedParamsPort;
	private final OrderCheckoutCartPort orderCheckoutCartPort;
	private final OrderCreateItemCheckPort orderCreateItemCheckPort;
	private final OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;
	private final OrderCreateFormatDataPort orderCreateFormatDataPort;
	private final ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner;
	private final ShopadminDeferredOrderCache shopadminDeferredOrderCache;
	private final NormalOrderNumericIdService normalOrderNumericIdService;

	public ShopadminNormalOrderCreateOrchestrator(
			OrderCreateNeedParamsPort orderCreateNeedParamsPort,
			OrderCheckoutCartPort orderCheckoutCartPort,
			OrderCreateItemCheckPort orderCreateItemCheckPort,
			OrderCreateDistributorCheckPort orderCreateDistributorCheckPort,
			OrderCreateFormatDataPort orderCreateFormatDataPort,
			ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner,
			ShopadminDeferredOrderCache shopadminDeferredOrderCache,
			NormalOrderNumericIdService normalOrderNumericIdService) {
		this.orderCreateNeedParamsPort = orderCreateNeedParamsPort;
		this.orderCheckoutCartPort = orderCheckoutCartPort;
		this.orderCreateItemCheckPort = orderCreateItemCheckPort;
		this.orderCreateDistributorCheckPort = orderCreateDistributorCheckPort;
		this.orderCreateFormatDataPort = orderCreateFormatDataPort;
		this.shopadminNormalOrderCreateTransactionalRunner = shopadminNormalOrderCreateTransactionalRunner;
		this.shopadminDeferredOrderCache = shopadminDeferredOrderCache;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
	}

	public Map<String, Object> create(NormalOrderCreateState state, HttpServletRequest request) {
		assertKnownOrderType(state);
		orderCreateNeedParamsPort.validate(state);
		orderCheckoutCartPort.fillItemsFromOperatorCart(state, request);
		orderCreateItemCheckPort.check(state);
		orderCreateDistributorCheckPort.check(state);
		orderCreateFormatDataPort.format(state);
		orderCreateFormatDataPort.assertDepositBalance(state);
		shopadminNormalOrderCreateTransactionalRunner.preparePricingSnapshot(state);

		Map<String, Object> od = state.getOrderData();
		long companyId = longVal(od.get("company_id"), longVal(state.getParams().get("company_id"), 0L));
		long userId = longVal(od.get("user_id"), longVal(state.getParams().get("user_id"), 0L));
		long orderId = normalOrderNumericIdService.generate(userId);
		od.put("order_id", orderId);

		String payType = stringVal(od.get("pay_type"));
		if (!StringUtils.hasText(payType)) {
			payType = stringVal(state.getParams().get("pay_type"));
		}
		if (!StringUtils.hasText(payType)) {
			payType = "pos";
		}
		int totalFee = (int) Math.min(longVal(od.get("total_fee"), 0L), Integer.MAX_VALUE);
		int ttlSeconds = resolveWaitPayTtlSeconds(od);
		shopadminDeferredOrderCache.saveWaitPay(state, orderId, companyId, payType, totalFee, ttlSeconds);
		return buildDeferredCreatePayload(state, orderId, companyId);
	}

	private static void assertKnownOrderType(NormalOrderCreateState state) {
		String ot = String.valueOf(state.getParams().getOrDefault("order_type", ""));
		if (!"normal_shopadmin".equals(ot)) {
			throw new ResourceException("无此类型订单！");
		}
	}

	private static int resolveWaitPayTtlSeconds(Map<String, Object> od) {
		long autoCancel = longVal(od.get("auto_cancel_time"), 0L);
		long now = System.currentTimeMillis() / 1000L;
		if (autoCancel > now) {
			long delta = autoCancel - now;
			return (int) Math.min(delta, Integer.MAX_VALUE);
		}
		return 15 * 60;
	}

	private static Map<String, Object> buildDeferredCreatePayload(
			NormalOrderCreateState state, long orderId, long companyId) {
		Map<String, Object> res = new LinkedHashMap<>();
		Map<String, Object> od = state.getOrderData();
		Map<String, Object> pr = state.getParams();
		res.put("order_id", orderId);
		res.put("company_id", companyId);
		res.put("order_status", "NOTPAY");
		res.put("pay_status", "NOTPAY");
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

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
