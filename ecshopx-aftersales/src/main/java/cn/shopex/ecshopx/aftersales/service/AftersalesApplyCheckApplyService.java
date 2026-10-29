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
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AftersalesApplyCheckApplyService {

	private static final String MSG_ORDER_NOT_FOUND = "aftersales.apply.order_not_found";
	private static final String MSG_ORDER_STATUS_INVALID = "aftersales.apply.order_status_invalid";
	private static final String MSG_APPLY_EXPIRED = "aftersales.apply.apply_expired";
	private static final String MSG_DETAIL_REQUIRED = "aftersales.apply.detail_required";
	private static final String MSG_TYPE_REQUIRED = "aftersales.apply.type_required";
	private static final String MSG_REASON_REQUIRED = "aftersales.apply.reason_required";
	private static final String MSG_SUB_ORDER_NOT_FOUND = "aftersales.apply.sub_order_not_found";
	private static final String MSG_QUANTITY_MUST_POSITIVE = "aftersales.apply.quantity_must_positive";
	private static final String MSG_ORDER_NOT_VERIFIED = "aftersales.apply.order_not_verified";
	private static final String MSG_NOT_DELIVERED = "aftersales.apply.not_delivered";
	private static final String MSG_QUANTITY_EXCEEDED = "aftersales.apply.quantity_exceeded";
	private static final String MSG_ORDER_ITEMS_NOT_FOUND = "aftersales.apply.order_items_not_found";
	private static final String MSG_FREIGHT_PARTIAL_NOT_SUPPORTED =
			"aftersales.apply.freight_partial_not_supported";
	private static final String MSG_FREIGHT_NOT_PAID = "aftersales.apply.freight_not_paid";
	private static final String MSG_FREIGHT_EXCEEDED = "aftersales.apply.freight_exceeded";
	private static final String MSG_FREIGHT_POINT_NOT_PAID = "aftersales.apply.freight_point_not_paid";
	private static final String MSG_FREIGHT_POINT_EXCEEDED = "aftersales.apply.freight_point_exceeded";

	private static final String DEFAULT_ORDER_NOT_FOUND = "系统无此订单，无法申请售后";
	private static final String DEFAULT_ORDER_STATUS_INVALID = "该订单不能申请售后";
	private static final String DEFAULT_APPLY_EXPIRED = "该订单已超过售后申请时效";
	private static final String DEFAULT_DETAIL_REQUIRED = "请提交审核售后的商品";
	private static final String DEFAULT_TYPE_REQUIRED = "售后类型必选";
	private static final String DEFAULT_REASON_REQUIRED = "售后理由必选";
	private static final String DEFAULT_SUB_ORDER_NOT_FOUND = "申请售后商品的订单不存在";
	private static final String DEFAULT_QUANTITY_MUST_POSITIVE = "{0} 售后的商品数量必须大于0";
	private static final String DEFAULT_ORDER_NOT_VERIFIED = "请先核销订单";
	private static final String DEFAULT_NOT_DELIVERED = "{0} 未发货，不能申请退换货";
	private static final String DEFAULT_QUANTITY_EXCEEDED = "{0} 剩余可申请售后的数量为{1}";
	private static final String DEFAULT_ORDER_ITEMS_NOT_FOUND = "订单商品不存在";
	private static final String DEFAULT_FREIGHT_PARTIAL_NOT_SUPPORTED = "部分退不支持退运费";
	private static final String DEFAULT_FREIGHT_NOT_PAID = "订单未付运费";
	private static final String DEFAULT_FREIGHT_EXCEEDED = "申请退款运费不能超出支付运费";
	private static final String DEFAULT_FREIGHT_POINT_NOT_PAID = "订单未付积分运费";
	private static final String DEFAULT_FREIGHT_POINT_EXCEEDED =
			"申请退款积分运费不能超出支付积分运费";

	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService;
	private final MessageSource messageSource;

	public AftersalesApplyCheckApplyService(
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService,
			MessageSource messageSource) {
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.aftersalesApplyDetailQueryService = aftersalesApplyDetailQueryService;
		this.messageSource = messageSource;
	}

	public void checkApply(AftersalesApplyParams params) {
		checkApply(params, false);
	}

	public void checkApply(
			AftersalesApplyParams params, boolean shareFullDeliveredAftersalesQuantity) {
		Map<String, Object> data = params.toCheckApplyDataMap();
		long companyId = longVal(data.get("company_id"));
		long orderId = longVal(data.get("order_id"));

		Optional<Map<String, Object>> headOpt =
				orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (headOpt.isEmpty()) {
			throw new ResourceException(message(MSG_ORDER_NOT_FOUND, DEFAULT_ORDER_NOT_FOUND));
		}
		Map<String, Object> orderInfo = headOpt.get();
		String orderStatus = str(orderInfo.get("order_status"));
		if ("NOTPAY".equals(orderStatus) || "CANCEL".equals(orderStatus)) {
			throw new ResourceException(
					message(MSG_ORDER_STATUS_INVALID, DEFAULT_ORDER_STATUS_INVALID));
		}
		if ("DONE".equals(orderStatus)) {
			int closeAt = intVal(orderInfo.get("order_auto_close_aftersales_time"));
			if (closeAt > 0 && System.currentTimeMillis() / 1000L > closeAt) {
				throw new ResourceException(message(MSG_APPLY_EXPIRED, DEFAULT_APPLY_EXPIRED));
			}
		}

		Object detailObj = data.get("detail");
		if (!(detailObj instanceof List<?> list) || list.isEmpty()) {
			throw new ResourceException(message(MSG_DETAIL_REQUIRED, DEFAULT_DETAIL_REQUIRED));
		}

		String aftersalesType = str(data.get("aftersales_type"));
		if (aftersalesType.isEmpty()) {
			throw new ResourceException(message(MSG_TYPE_REQUIRED, DEFAULT_TYPE_REQUIRED));
		}
		if (str(data.get("reason")).isEmpty()) {
			throw new ResourceException(message(MSG_REASON_REQUIRED, DEFAULT_REASON_REQUIRED));
		}

		List<Map<String, Object>> orderLineList =
				orderNormalOrderItemsReadPort.listItems(companyId, orderId);

		int freightReq = intVal(data.get("freight"));
		if (isFreightRefundType(aftersalesType) && freightReq > 0) {
			if (orderLineList == null || orderLineList.isEmpty()) {
				throw new ResourceException(
						message(MSG_ORDER_ITEMS_NOT_FOUND, DEFAULT_ORDER_ITEMS_NOT_FOUND));
			}
			Map<Long, Integer> applyDetailMap = new HashMap<>();
			for (Object o : list) {
				if (!(o instanceof Map<?, ?> m)) {
					continue;
				}
				long sid = longVal(m.get("id"));
				int n = intVal(m.get("num"));
				applyDetailMap.merge(sid, n, Integer::sum);
			}
			boolean isLastAftersales = true;
			for (Map<String, Object> orderItem : orderLineList) {
				if ("gift".equals(str(orderItem.get("order_item_type")))) {
					continue;
				}
				long orderItemId = longVal(orderItem.get("id"));
				int orderItemNum = intVal(orderItem.get("num"));
				int appliedNum =
						aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderId, orderItemId);
				Integer applyN = applyDetailMap.get(orderItemId);
				if (applyN != null) {
					if (appliedNum + applyN < orderItemNum) {
						isLastAftersales = false;
						break;
					}
				} else {
					if (appliedNum < orderItemNum) {
						isLastAftersales = false;
						break;
					}
				}
			}
			if (!isLastAftersales) {
				throw new ResourceException(
						message(
								MSG_FREIGHT_PARTIAL_NOT_SUPPORTED,
								DEFAULT_FREIGHT_PARTIAL_NOT_SUPPORTED));
			}
			String ft = str(orderInfo.get("freight_type"));
			if ("cash".equals(ft)) {
				int ff = intVal(orderInfo.get("freight_fee"));
				if (ff <= 0) {
					throw new ResourceException(message(MSG_FREIGHT_NOT_PAID, DEFAULT_FREIGHT_NOT_PAID));
				}
				int refunded =
						aftersalesApplyDetailQueryService.sumAppliedFreightCash(companyId, orderId);
				int remain = ff - refunded;
				if (freightReq > remain) {
					throw new ResourceException(
							message(MSG_FREIGHT_EXCEEDED, DEFAULT_FREIGHT_EXCEEDED));
				}
			} else if ("point".equals(ft)) {
				int ff = intVal(orderInfo.get("freight_fee"));
				if (ff <= 0) {
					throw new ResourceException(
							message(MSG_FREIGHT_POINT_NOT_PAID, DEFAULT_FREIGHT_POINT_NOT_PAID));
				}
				int refunded =
						aftersalesApplyDetailQueryService.sumAppliedFreightPoint(companyId, orderId);
				int remain = ff - refunded;
				if (freightReq > remain) {
					throw new ResourceException(
							message(MSG_FREIGHT_POINT_EXCEEDED, DEFAULT_FREIGHT_POINT_EXCEEDED));
				}
			}
		}

		for (Object o : list) {
			if (!(o instanceof Map<?, ?> raw)) {
				throw new ResourceException(message(MSG_DETAIL_REQUIRED, DEFAULT_DETAIL_REQUIRED));
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) raw;
			long subId = longVal(v.get("id"));
			Map<String, Object> subOrderInfo = findSubOrder(orderLineList, subId);
			if (subOrderInfo == null) {
				throw new ResourceException(
						message(MSG_SUB_ORDER_NOT_FOUND, DEFAULT_SUB_ORDER_NOT_FOUND));
			}
			int num = intVal(v.get("num"));
			if (num <= 0) {
				throw new ResourceException(
						message(
								MSG_QUANTITY_MUST_POSITIVE,
								DEFAULT_QUANTITY_MUST_POSITIVE,
								str(subOrderInfo.get("item_name"))));
			}
			if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
				if (!"DONE".equals(str(orderInfo.get("delivery_status")))) {
					throw new ResourceException(message(MSG_ORDER_NOT_VERIFIED, DEFAULT_ORDER_NOT_VERIFIED));
				}
			} else {
				int deliveryItemNum = intVal(subOrderInfo.get("delivery_item_num"));
				if ("DONE".equals(str(subOrderInfo.get("delivery_status"))) && deliveryItemNum <= 0) {
					deliveryItemNum = intVal(subOrderInfo.get("num"));
				}
				String ast = str(data.get("aftersales_type"));
				if ("REFUND_GOODS".equals(ast) || "EXCHANGING_GOODS".equals(ast)) {
					if (deliveryItemNum <= 0) {
						throw new ResourceException(
								message(
										MSG_NOT_DELIVERED,
										DEFAULT_NOT_DELIVERED,
										str(subOrderInfo.get("item_name"))));
					}
				}
			}
			boolean partial = Boolean.TRUE.equals(data.get("is_partial_cancel"));
			if (!partial) {
				int lineNum = intVal(subOrderInfo.get("num"));
				int deliveryItemNum = intVal(subOrderInfo.get("delivery_item_num"));
				if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
					deliveryItemNum = lineNum;
				} else {
					if ("DONE".equals(str(subOrderInfo.get("delivery_status"))) && deliveryItemNum <= 0) {
						deliveryItemNum = lineNum;
					}
				}
				deliveryItemNum = Math.min(deliveryItemNum, lineNum);
				int cancelItemNum = intVal(subOrderInfo.get("cancel_item_num"));
				int appliedNum;
				int leftNum;
				if ("ONLY_REFUND".equals(aftersalesType)
						&& shareFullDeliveredAftersalesQuantity
						&& "DONE".equals(str(orderInfo.get("delivery_status")))) {
					appliedNum =
							aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderId, subId);
					leftNum = deliveryItemNum - appliedNum;
				} else if ("ONLY_REFUND".equals(aftersalesType)) {
					appliedNum =
							aftersalesApplyDetailQueryService.sumAppliedNumByType(
									companyId, orderId, subId, "ONLY_REFUND");
					leftNum = lineNum - cancelItemNum - deliveryItemNum - appliedNum;
				} else {
					appliedNum =
							aftersalesApplyDetailQueryService.sumAppliedNumByType(
									companyId, orderId, subId, "REFUND_GOODS", "EXCHANGING_GOODS");
					leftNum = deliveryItemNum - appliedNum;
				}
				if (num > leftNum) {
					throw new ResourceException(
							message(
									MSG_QUANTITY_EXCEEDED,
									DEFAULT_QUANTITY_EXCEEDED,
									str(subOrderInfo.get("item_name")),
									leftNum));
				}
			}
		}
	}

	private String message(String key, String defaultMessage, Object... args) {
		Locale locale = LocaleContextHolder.getLocale();
		if (locale == null) {
			locale = Locale.SIMPLIFIED_CHINESE;
		}
		return messageSource.getMessage(key, args.length == 0 ? null : args, defaultMessage, locale);
	}

	private static Map<String, Object> findSubOrder(List<Map<String, Object>> items, long subId) {
		if (items == null) {
			return null;
		}
		for (Map<String, Object> it : items) {
			if (longVal(it.get("id")) == subId) {
				return it;
			}
		}
		return null;
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static boolean isFreightRefundType(String aftersalesType) {
		return "REFUND_GOODS".equals(aftersalesType) || "EXCHANGING_GOODS".equals(aftersalesType);
	}
}
