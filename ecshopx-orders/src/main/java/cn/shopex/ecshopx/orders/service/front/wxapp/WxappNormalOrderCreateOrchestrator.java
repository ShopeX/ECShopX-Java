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

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutEmployeePurchaseCartPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateDistributorCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateEmployeePurchaseFormatPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateFormatDataPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsNormalCheckoutPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.common.order.normal.OrderCreateNeedParamsPort;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappNormalOrderCreateOrchestrator {

	private static final Pattern CN_ID_CARD =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[xX\\d]$");

	private final OrderCreateNeedParamsPort orderCreateNeedParamsPort;
	private final OrderCheckoutCartPort orderCheckoutCartPort;
	private final OrderCheckoutEmployeePurchaseCartPort orderCheckoutEmployeePurchaseCartPort;
	private final OrderCreateEmployeePurchaseFormatPort orderCreateEmployeePurchaseFormatPort;
	private final OrderCreateItemCheckPort orderCreateItemCheckPort;
	private final OrderCreateDistributorCheckPort orderCreateDistributorCheckPort;
	private final OrderCreateFormatDataPort orderCreateFormatDataPort;
	private final OrderCreateGroupsNormalCheckoutPort orderCreateGroupsNormalCheckoutPort;
	private final WxappNormalOrderTempInfoEnrichmentService wxappNormalOrderTempInfoEnrichmentService;
	private final WxappNormalOrderCreateTransactionalRunner wxappNormalOrderCreateTransactionalRunner;
	private final NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher;
	private final SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService;

	public WxappNormalOrderCreateOrchestrator(
			OrderCreateNeedParamsPort orderCreateNeedParamsPort,
			OrderCheckoutCartPort orderCheckoutCartPort,
			OrderCheckoutEmployeePurchaseCartPort orderCheckoutEmployeePurchaseCartPort,
			OrderCreateEmployeePurchaseFormatPort orderCreateEmployeePurchaseFormatPort,
			OrderCreateItemCheckPort orderCreateItemCheckPort,
			OrderCreateDistributorCheckPort orderCreateDistributorCheckPort,
			OrderCreateFormatDataPort orderCreateFormatDataPort,
			OrderCreateGroupsNormalCheckoutPort orderCreateGroupsNormalCheckoutPort,
			WxappNormalOrderTempInfoEnrichmentService wxappNormalOrderTempInfoEnrichmentService,
			WxappNormalOrderCreateTransactionalRunner wxappNormalOrderCreateTransactionalRunner,
			NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher,
			SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService) {
		this.orderCreateNeedParamsPort = orderCreateNeedParamsPort;
		this.orderCheckoutCartPort = orderCheckoutCartPort;
		this.orderCheckoutEmployeePurchaseCartPort = orderCheckoutEmployeePurchaseCartPort;
		this.orderCreateEmployeePurchaseFormatPort = orderCreateEmployeePurchaseFormatPort;
		this.orderCreateItemCheckPort = orderCreateItemCheckPort;
		this.orderCreateDistributorCheckPort = orderCreateDistributorCheckPort;
		this.orderCreateFormatDataPort = orderCreateFormatDataPort;
		this.orderCreateGroupsNormalCheckoutPort = orderCreateGroupsNormalCheckoutPort;
		this.wxappNormalOrderTempInfoEnrichmentService = wxappNormalOrderTempInfoEnrichmentService;
		this.wxappNormalOrderCreateTransactionalRunner = wxappNormalOrderCreateTransactionalRunner;
		this.normalOrderAddDispatchPublisher = normalOrderAddDispatchPublisher;
		this.supplierOrderSplitOnNormalOrderAddService = supplierOrderSplitOnNormalOrderAddService;
	}

	public Map<String, Object> create(NormalOrderCreateState state, HttpServletRequest request, String expectedOrderType) {
		assertKnownOrderType(state, expectedOrderType);
		orderCreateNeedParamsPort.validate(state);
		fillCheckoutCart(state, request, expectedOrderType);
		if (isStoreAdjustBlockCheckout(state)) {
			Object tip = state.getParams().get("store_quantity_adjust_tip");
			String msg =
					tip != null && StringUtils.hasText(tip.toString())
							? tip.toString().trim()
							: "当前配送方式下商品暂无库存，请切换配送方式";
			throw new ResourceException(msg);
		}
		orderCreateItemCheckPort.check(state);
		orderCreateDistributorCheckPort.check(state);
		orderCreateFormatDataPort.format(state);
		applyEmployeePurchaseFormatIfNeeded(state, expectedOrderType, true);
		if ("normal_groups".equals(expectedOrderType)) {
			orderCreateGroupsNormalCheckoutPort.applyAfterFormat(state);
		}
		wxappNormalOrderTempInfoEnrichmentService.applyLogisticsReceiverAndFreightFromParams(state);
		orderCreateFormatDataPort.applyCheckoutCouponAfterFreight(state);
		wxappNormalOrderTempInfoEnrichmentService.applyDefaultCurrency(state);
		assertEmployeePurchasePrepaidQuotaAfterFreight(state, expectedOrderType, true);
		wxappNormalOrderCreateTransactionalRunner.runInTransaction(state, request);
		splitSupplierOrders(state);
		normalOrderAddDispatchPublisher.publish(buildNormalOrderAddPayload(state));
		return buildSuccessPayload(state);
	}

	public Map<String, Object> getOrderTempInfo(
			NormalOrderCreateState state, HttpServletRequest request, String expectedOrderType) {
		assertKnownOrderType(state, expectedOrderType);
		orderCreateNeedParamsPort.checkCreateOrderNeedParamsForTempInfo(state.getParams(), false);
		fillCheckoutCart(state, request, expectedOrderType);
		if (isStoreAdjustBlockCheckout(state)) {
			return buildStoreAdjustBlockPayload(state);
		}
		orderCreateItemCheckPort.check(state);
		orderCreateDistributorCheckPort.check(state);
		orderCreateFormatDataPort.format(state);
		applyEmployeePurchaseFormatIfNeeded(state, expectedOrderType, false);
		applyCrossBorderIdentityAfterFormatForTempInfo(state);
		if ("normal_groups".equals(expectedOrderType)) {
			orderCreateGroupsNormalCheckoutPort.applyAfterFormat(state);
		}
		if ("normal_pointsmall".equals(expectedOrderType)) {
			wxappNormalOrderTempInfoEnrichmentService.applyForOnlineWxappPointsmallPhysical(state);
		} else {
			wxappNormalOrderTempInfoEnrichmentService.applyForOnlineWxappNormalPhysical(state);
		}
		assertEmployeePurchasePrepaidQuotaAfterFreight(state, expectedOrderType, false);
		Map<String, Object> res = buildTempInfoPayload(state);
		enrichTempInfoInvoiceKeys(state, res);
		return res;
	}

	private void fillCheckoutCart(NormalOrderCreateState state, HttpServletRequest request, String expectedOrderType) {
		if ("normal_employee_purchase".equals(expectedOrderType)) {
			orderCheckoutEmployeePurchaseCartPort.fillItemsFromEmployeePurchaseCart(state, request);
		} else if ("normal_groups".equals(expectedOrderType)) {
			orderCheckoutCartPort.fillItemsFromGroupsFastBuyCart(state, request);
		} else if (usesMemberFastBuyCart(expectedOrderType)) {
			orderCheckoutCartPort.fillItemsFromMemberDistributorCart(state, request);
		} else if ("normal_pointsmall".equals(expectedOrderType)) {
			orderCheckoutCartPort.fillItemsFromMemberPointsmallCart(state, request);
		} else if (shouldFillOperatorCart(expectedOrderType)) {
			orderCheckoutCartPort.fillItemsFromOperatorCart(state, request);
		}
	}

	private void applyEmployeePurchaseFormatIfNeeded(
			NormalOrderCreateState state, String expectedOrderType, boolean isCheck) {
		if (!"normal_employee_purchase".equals(expectedOrderType)) {
			return;
		}
		orderCreateEmployeePurchaseFormatPort.applyEmployeePurchaseFormat(state, isCheck);
	}

	private void assertEmployeePurchasePrepaidQuotaAfterFreight(
			NormalOrderCreateState state, String expectedOrderType, boolean isCheck) {
		if (!"normal_employee_purchase".equals(expectedOrderType)) {
			return;
		}
		orderCreateEmployeePurchaseFormatPort.assertPrepaidPointQuotaAfterFreight(state, isCheck);
	}

	private static boolean usesMemberFastBuyCart(String orderTypeSlug) {
		return "normal".equals(orderTypeSlug);
	}

	private static boolean shouldFillOperatorCart(String orderTypeSlug) {
		return switch (orderTypeSlug) {
			case "service",
					"service_groups",
					"groups",
					"service_seckill",
					"bargain",
					"normal_excard",
					"normal_community",
					"normal_pointsmall",
					"normal_employee_purchase" -> false;
			default -> true;
		};
	}

	private static void applyCrossBorderIdentityAfterFormatForTempInfo(NormalOrderCreateState state) {
		Map<String, Object> pr = state.getParams();
		if (!pr.containsKey("iscrossborder")) {
			return;
		}
		if (intFromCrossBorder(pr.get("iscrossborder")) != 1) {
			return;
		}
		String identityId = String.valueOf(pr.getOrDefault("identity_id", "")).trim();
		if (!CN_ID_CARD.matcher(identityId).matches()) {
			throw new BadRequestException("身份证格式错误");
		}
		Map<String, Object> od = state.getOrderData();
		od.put("identity_id", identityId);
		od.put("identity_name", String.valueOf(pr.getOrDefault("identity_name", "")));
		od.put("type", 1);
	}

	private static int intFromCrossBorder(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static void enrichTempInfoInvoiceKeys(NormalOrderCreateState state, Map<String, Object> res) {
		Map<String, Object> od = state.getOrderData();
		if (od != null && od.containsKey("order_status")) {
			res.put("order_status", od.get("order_status"));
		} else {
			res.putIfAbsent("order_status", "NOTPAY");
		}
		if (od != null && od.containsKey("cancel_status")) {
			res.put("cancel_status", od.get("cancel_status"));
		}
		Object cid = state.getParams().get("company_id");
		if (cid != null) {
			res.putIfAbsent("company_id", cid);
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

	private static void assertKnownOrderType(NormalOrderCreateState state, String expected) {
		String ot = String.valueOf(state.getParams().getOrDefault("order_type", "")).trim();
		if (!expected.equals(ot)) {
			throw new ResourceException("无此类型订单！");
		}
	}

	private static boolean isStoreAdjustBlockCheckout(NormalOrderCreateState state) {
		Object flag = state.getParams().get("store_adjust_block_checkout");
		if (Boolean.TRUE.equals(flag)) {
			return true;
		}
		if (flag instanceof Number n) {
			return n.intValue() != 0;
		}
		return flag != null && ("1".equals(flag.toString().trim()) || "true".equalsIgnoreCase(flag.toString().trim()));
	}

	/** 整单不可结算时短路返回，避免空 items 进入 check/format。 */
	private static Map<String, Object> buildStoreAdjustBlockPayload(NormalOrderCreateState state) {
		Map<String, Object> pr = state.getParams();
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("items", List.of());
		res.put("item_fee", 0);
		res.put("total_fee", 0);
		res.put("freight_fee", 0);
		res.put("discount_fee", 0);
		res.put("store_adjust_block_checkout", Boolean.TRUE);
		copyIfPresent(res, pr, "store_quantity_adjustments");
		copyIfPresent(res, pr, "store_quantity_adjust_tip");
		copyIfPresent(res, pr, "suggested_receipt_type");
		copyIfPresent(res, pr, "receipt_type");
		copyIfPresent(res, pr, "company_id");
		copyIfPresent(res, pr, "user_id");
		copyIfPresent(res, pr, "distributor_id");
		copyIfPresent(res, pr, "order_type");
		copyIfPresent(res, pr, "cart_type");
		return res;
	}

	private static Map<String, Object> buildTempInfoPayload(NormalOrderCreateState state) {
		Map<String, Object> pr = state.getParams();
		if (Boolean.TRUE.equals(pr.get("is_online_order"))) {
			String ot = String.valueOf(pr.getOrDefault("order_type", "")).trim();
			if ("normal".equals(ot)
					|| "normal_pointsmall".equals(ot)
					|| "normal_groups".equals(ot)
					|| "normal_employee_purchase".equals(ot)) {
				Map<String, Object> res = copyOrderDataToResponse(state.getOrderData());
				if ("normal_groups".equals(ot)) {
					res.remove("receipt_type");
				}
				Object receiptType = res.get("receipt_type");
				if (receiptType != null && "logistics".equalsIgnoreCase(String.valueOf(receiptType).trim())) {
					res.remove("receipt_type");
				}
				return res;
			}
		}
		return buildSuccessPayload(state);
	}

	private static Map<String, Object> copyOrderDataToResponse(Map<String, Object> od) {
		Map<String, Object> res = new LinkedHashMap<>();
		if (od == null) {
			return res;
		}
		res.putAll(od);
		return res;
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
			copyIfPresent(res, od, "purchase_mode");
			copyIfPresent(res, od, "prepaid_payable_fee");
			copyIfPresent(res, od, "freight_type");
			copyIfPresent(res, od, "freight_fee");
			copyIfPresent(res, od, "receipt_type");
			copyIfPresent(res, od, "point_fee");
			copyIfPresent(res, od, "point_use");
			copyIfPresent(res, od, "operator_id");
			copyIfPresent(res, od, "salesman_id");
			copyIfPresent(res, od, "distributor_id");
			copyIfPresent(res, od, "wxa_appid");
			copyIfPresent(res, od, "authorizer_appid");
			copyIfPresent(res, od, "fee_rate");
			copyIfPresent(res, od, "fee_type");
			copyIfPresent(res, od, "fee_symbol");
			copyIfPresent(res, od, "prescription_status");
			copyIfPresent(res, od, "team_id");
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
