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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AftersalesApplyCheckApplyService {

	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService;

	public AftersalesApplyCheckApplyService(
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService) {
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.aftersalesApplyDetailQueryService = aftersalesApplyDetailQueryService;
	}

	public void checkApply(AftersalesApplyParams params) {
		Map<String, Object> data = params.toCheckApplyDataMap();
		long companyId = longVal(data.get("company_id"));
		long orderId = longVal(data.get("order_id"));

		Optional<Map<String, Object>> headOpt =
				orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (headOpt.isEmpty()) {
			throw new ResourceException("系统无此订单，无法申请售后");
		}
		Map<String, Object> orderInfo = headOpt.get();
		String orderStatus = str(orderInfo.get("order_status"));
		if ("NOTPAY".equals(orderStatus) || "CANCEL".equals(orderStatus)) {
			throw new ResourceException("该订单不能申请售后");
		}
		if ("DONE".equals(orderStatus)) {
			int closeAt = intVal(orderInfo.get("order_auto_close_aftersales_time"));
			if (closeAt > 0 && System.currentTimeMillis() / 1000L > closeAt) {
				throw new ResourceException("该订单已超过售后申请时效");
			}
		}

		Object detailObj = data.get("detail");
		if (!(detailObj instanceof List<?> list) || list.isEmpty()) {
			throw new ResourceException("请提交审核售后的商品");
		}

		if (str(data.get("aftersales_type")).isEmpty()) {
			throw new ResourceException("售后类型必选");
		}
		if (str(data.get("reason")).isEmpty()) {
			throw new ResourceException("售后原因必选");
		}

		List<Map<String, Object>> orderLineList =
				orderNormalOrderItemsReadPort.listItems(companyId, orderId);

		int freightReq = intVal(data.get("freight"));
		if (freightReq > 0) {
			if (orderLineList == null || orderLineList.isEmpty()) {
				throw new ResourceException("订单商品不存在");
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
				throw new ResourceException("部分退不支持退运费");
			}
			String ft = str(orderInfo.get("freight_type"));
			if ("cash".equals(ft)) {
				int ff = intVal(orderInfo.get("freight_fee"));
				if (ff <= 0) {
					throw new ResourceException("订单未付运费");
				}
				int refunded =
						aftersalesApplyDetailQueryService.sumAppliedFreightCash(companyId, orderId);
				int remain = ff - refunded;
				if (freightReq > remain) {
					throw new ResourceException("申请退款运费不能超出支付运费");
				}
			} else if ("point".equals(ft)) {
				int ff = intVal(orderInfo.get("freight_point"));
				if (ff <= 0) {
					throw new ResourceException("订单未付积分运费");
				}
				int refunded =
						aftersalesApplyDetailQueryService.sumAppliedFreightPoint(companyId, orderId);
				int remain = ff - refunded;
				if (freightReq > remain) {
					throw new ResourceException("申请退款积分运费不能超出支付积分运费");
				}
			}
		}

		for (Object o : list) {
			if (!(o instanceof Map<?, ?> raw)) {
				throw new ResourceException("请提交审核售后的商品");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) raw;
			long subId = longVal(v.get("id"));
			Map<String, Object> subOrderInfo = findSubOrder(orderLineList, subId);
			if (subOrderInfo == null) {
				throw new ResourceException("申请售后商品的订单不存在");
			}
			int num = intVal(v.get("num"));
			if (num <= 0) {
				throw new ResourceException(str(subOrderInfo.get("item_name")) + " 售后的商品数量必须大于0");
			}
			if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
				if (!"DONE".equals(str(orderInfo.get("delivery_status")))) {
					throw new ResourceException("请先核销订单");
				}
			} else {
				int deliveryItemNum = intVal(subOrderInfo.get("delivery_item_num"));
				if ("DONE".equals(str(subOrderInfo.get("delivery_status"))) && deliveryItemNum <= 0) {
					deliveryItemNum = intVal(subOrderInfo.get("num"));
				}
				String ast = str(data.get("aftersales_type"));
				if ("REFUND_GOODS".equals(ast) || "EXCHANGING_GOODS".equals(ast)) {
					if (deliveryItemNum <= 0) {
						throw new ResourceException(str(subOrderInfo.get("item_name")) + " 未发货，不能申请退换货");
					}
				}
			}
			boolean partial = Boolean.TRUE.equals(data.get("is_partial_cancel"));
			if (!partial) {
				int appliedNum =
						aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderId, subId);
				int deliveryItemNum = intVal(subOrderInfo.get("delivery_item_num"));
				if ("ziti".equals(str(orderInfo.get("receipt_type")))) {
					deliveryItemNum = intVal(subOrderInfo.get("num"));
				} else {
					if ("DONE".equals(str(subOrderInfo.get("delivery_status"))) && deliveryItemNum <= 0) {
						deliveryItemNum = intVal(subOrderInfo.get("num"));
					}
				}
				int cancelItemNum = intVal(subOrderInfo.get("cancel_item_num"));
				int leftNum = deliveryItemNum + cancelItemNum - appliedNum;
				if (num > leftNum) {
					throw new ResourceException(
							str(subOrderInfo.get("item_name"))
									+ " 剩余可申请售后的数量为"
									+ leftNum
									+ ",申请售后数量为"
									+ num
									+ ",已申请售后数量为:"
									+ appliedNum);
				}
			}
		}

		validateRequestRefundAgainstComputedTotals(companyId, orderId, list, orderLineList, data);
	}

	private void validateRequestRefundAgainstComputedTotals(
			long companyId,
			long orderId,
			List<?> detailList,
			List<Map<String, Object>> orderLineList,
			Map<String, Object> data) {
		BigDecimal totalRefundFeeBd = BigDecimal.ZERO;
		BigDecimal totalRefundPointBd = BigDecimal.ZERO;

		for (Object o : detailList) {
			if (!(o instanceof Map<?, ?> raw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) raw;
			long subId = longVal(v.get("id"));
			Map<String, Object> subOrderInfo = findSubOrder(orderLineList, subId);
			if (subOrderInfo == null) {
				throw new ResourceException("申请售后商品的订单不存在");
			}
			int num = intVal(v.get("num"));
			int lineNum = intVal(subOrderInfo.get("num"));
			BigDecimal lineTotalFeeBd = bdInt(subOrderInfo.get("total_fee"));
			BigDecimal linePointBd = bdInt(subOrderInfo.get("point"));
			int appliedNum = aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderId, subId);
			int appliedRefundFee =
					aftersalesApplyDetailQueryService.sumAppliedRefundFee(companyId, orderId, subId);
			int appliedRefundPoint =
					aftersalesApplyDetailQueryService.sumAppliedRefundPoint(companyId, orderId, subId);

			BigDecimal refundFeeBd;
			BigDecimal refundPointBd;
			if (num == lineNum) {
				refundFeeBd = lineTotalFeeBd;
				refundPointBd = linePointBd;
			} else {
				int leftNum = lineNum - appliedNum - num;
				if (leftNum == 0) {
					refundFeeBd = lineTotalFeeBd.subtract(bdInt(appliedRefundFee));
					refundPointBd = linePointBd.subtract(bdInt(appliedRefundPoint));
				} else if (leftNum > 0) {
					refundFeeBd = floorDivMulBd(lineTotalFeeBd, lineNum, num);
					refundPointBd = floorDivMulBd(linePointBd, lineNum, num);
				} else {
					throw new ResourceException("申请售后单数据异常");
				}
			}

			if (v.get("total_fee") != null && intVal(v.get("total_fee")) > 0) {
				totalRefundFeeBd = totalRefundFeeBd.add(bdInt(v.get("total_fee")));
			} else {
				totalRefundFeeBd = totalRefundFeeBd.add(refundFeeBd);
			}
			if (v.get("total_point") != null && intVal(v.get("total_point")) > 0) {
				totalRefundPointBd = totalRefundPointBd.add(bdInt(v.get("total_point")));
			} else {
				totalRefundPointBd = totalRefundPointBd.add(refundPointBd);
			}
		}

		// Amounts are fen/points integers; round request values before compare to absorb
		// JSON/JS float noise such as 440.00000000000006.
		int reqFee = parseRefundAsNonNegInt(data.get("refund_fee"));
		int reqPoint = parseRefundAsNonNegInt(data.get("refund_point"));
		int totalRefundFee = bdToNonNegIntPlain(totalRefundFeeBd);
		int totalRefundPoint = bdToNonNegIntPlain(totalRefundPointBd);

		if (reqFee > totalRefundFee) {
			throw new ResourceException(
					"退款金额不能超过可退金额!" + reqFee + " > " + totalRefundFee);
		}
		if (reqPoint > totalRefundPoint) {
			throw new ResourceException("退还积分不能超过可退积分");
		}
	}

	private static int parseRefundAsNonNegInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return new BigDecimal(s)
					.setScale(0, RoundingMode.HALF_UP)
					.max(BigDecimal.ZERO)
					.intValue();
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private static BigDecimal floorDivMulBd(BigDecimal total, int divisor, int mult) {
		if (divisor <= 0 || mult <= 0) {
			return BigDecimal.ZERO;
		}
		return total
				.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(mult))
				.setScale(0, RoundingMode.FLOOR);
	}

	private static BigDecimal bdInt(Object o) {
		return BigDecimal.valueOf(intVal(o));
	}

	private static int bdToNonNegIntPlain(BigDecimal b) {
		if (b == null) {
			return 0;
		}
		return b.setScale(0, RoundingMode.FLOOR).max(BigDecimal.ZERO).intValue();
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
}
