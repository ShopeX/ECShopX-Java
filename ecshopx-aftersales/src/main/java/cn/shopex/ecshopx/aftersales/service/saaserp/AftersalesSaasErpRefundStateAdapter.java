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

package cn.shopex.ecshopx.aftersales.service.saaserp;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundConfirmService;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import cn.shopex.ecshopx.common.port.order.OrderConfirmCancelPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundRefundSideEffects;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpRefundStatePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * SaaS ERP {@code ome_create_reimburse} mall state. {@link AftersalesRefundConfirmService#confirmRefund}
 * and {@link AftersalesReviewService#review} already publish the SaaS ERP aftersales update/cancel dispatches
 * that PHP {@code confirmRefund}/{@code review} emit; this adapter does not add {@code store.trade.refund.add}
 * or {@code store.trade.update}. {@code agreeRefundWithoutAftersales} suppresses nested trade-update dispatches
 * from order cancel while the inbound act performs {@code store.trade.update} once.
 */
@Service
public class AftersalesSaasErpRefundStateAdapter implements SaasErpRefundStatePort {

	private static final String OMS_REFUSE = "OMS拒绝退款";
	private static final String OMS_CONFIRM = "OMS确认退款";

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AftersalesRefundConfirmService confirmService;
	private final AftersalesReviewService reviewService;
	private final OrderAssociationReadPort orderAssociationReadPort;
	private final OrderConfirmCancelPort orderConfirmCancelPort;

	public AftersalesSaasErpRefundStateAdapter(
			AftersalesMapper aftersalesMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			AftersalesRefundConfirmService confirmService,
			AftersalesReviewService reviewService,
			OrderAssociationReadPort orderAssociationReadPort,
			OrderConfirmCancelPort orderConfirmCancelPort) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.confirmService = confirmService;
		this.reviewService = reviewService;
		this.orderAssociationReadPort = orderAssociationReadPort;
		this.orderConfirmCancelPort = orderConfirmCancelPort;
	}

	@Override
	public Optional<Map<String, Object>> findRefund(long companyId, String orderId, String refundId) {
		Long parsedOrderId = parseLong(orderId);
		Long parsedRefundBn = parseLong(refundId);
		if (parsedOrderId == null || parsedRefundBn == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<AftersalesRefund> query = new LambdaQueryWrapper<>();
		query.eq(AftersalesRefund::getOrderId, parsedOrderId).eq(AftersalesRefund::getRefundBn, parsedRefundBn);
		if (companyId > 0) {
			query.eq(AftersalesRefund::getCompanyId, companyId);
		}
		query.last("LIMIT 1");
		AftersalesRefund row = aftersalesRefundMapper.selectOne(query);
		return row == null ? Optional.empty() : Optional.of(refundToMap(row));
	}

	@Override
	public Optional<String> findAftersalesBnByRefundBn(String refundBn) {
		Long parsed = parseLong(refundBn);
		if (parsed == null) {
			return Optional.empty();
		}
		AftersalesRefund row =
				aftersalesRefundMapper.selectOne(
						new LambdaQueryWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getRefundBn, parsed)
								.last("LIMIT 1"));
		if (row == null || row.getAftersalesBn() == null || row.getAftersalesBn() <= 0L) {
			return Optional.empty();
		}
		return Optional.of(String.valueOf(row.getAftersalesBn()));
	}

	@Override
	public Optional<Map<String, Object>> findAftersales(long companyId, String aftersalesBn) {
		Long parsedBn = parseLong(aftersalesBn);
		if (parsedBn == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<Aftersales> query = new LambdaQueryWrapper<>();
		query.eq(Aftersales::getAftersalesBn, parsedBn);
		if (companyId > 0) {
			query.eq(Aftersales::getCompanyId, companyId);
		}
		query.last("LIMIT 1");
		Aftersales row = aftersalesMapper.selectOne(query);
		return row == null ? Optional.empty() : Optional.of(aftersalesToMap(row));
	}

	@Override
	public Optional<Map<String, Object>> findOrder(long companyId, String orderId) {
		Long parsedOrderId = parseLong(orderId);
		if (parsedOrderId == null) {
			return Optional.empty();
		}
		return orderAssociationReadPort.getAssociation(companyId, parsedOrderId);
	}

	@Override
	public Object refuseRefundWithoutAftersales(Map<String, Object> refund, Map<String, String> params) {
		return SaasErpInboundRefundSideEffects.runSuppressingOutboundTradeUpdate(
				() -> invokeConfirmOrderCancel(refund, params, false));
	}

	@Override
	public Object agreeRefundWithoutAftersales(Map<String, Object> refund, Map<String, String> params) {
		return SaasErpInboundRefundSideEffects.runSuppressingOutboundTradeUpdate(
				() -> invokeConfirmOrderCancel(refund, params, true));
	}

	@Override
	public Object confirmRefundGoods(Map<String, Object> aftersales, Map<String, String> params, boolean agree) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", longVal(aftersales.get("company_id")));
		body.put("aftersales_bn", longVal(aftersales.get("aftersales_bn")));
		if (agree) {
			body.put("check_refund", 1);
			body.put("refund_fee", moneyToFeeCents(params.get("money")));
			body.put("refund_memo", OMS_CONFIRM);
		} else {
			body.put("check_refund", 0);
			body.put("refund_memo", OMS_REFUSE);
		}
		return confirmService.confirmRefund(body);
	}

	@Override
	public Object reviewOnlyRefund(Map<String, Object> aftersales, Map<String, String> params, boolean agree) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_id", longVal(aftersales.get("company_id")));
		body.put("aftersales_bn", longVal(aftersales.get("aftersales_bn")));
		body.put("is_approved", agree ? 1 : 0);
		body.put("refund_point", 0);
		if (agree) {
			body.put("refund_fee", moneyToFeeCents(params.get("money")));
			body.put("refund_memo", OMS_CONFIRM);
		} else {
			body.put("refuse_reason", OMS_REFUSE);
		}
		return reviewService.review(body, null);
	}

	private Object invokeConfirmOrderCancel(
			Map<String, Object> refund, Map<String, String> params, boolean agree) {
		long companyId = longVal(refund.get("company_id"));
		long orderId = longVal(refund.get("order_id"));
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("check_cancel", agree ? "1" : "0");
		merged.put("company_id", companyId);
		merged.put("order_id", orderId);
		merged.put("refund_bn", refund.get("refund_bn"));
		merged.put("order_type", "normal");
		merged.put("operator_type", "system");
		merged.put("operator_id", 0L);
		if (agree) {
			String memo = params.get("memo");
			merged.put(
					"shop_reject_reason",
					memo == null || memo.isBlank() ? OMS_CONFIRM : memo.trim());
		} else {
			merged.put("shop_reject_reason", OMS_REFUSE);
		}
		return orderConfirmCancelPort.confirmOrderCancel(
				companyId, "system", 0L, String.valueOf(orderId), merged);
	}

	private static int moneyToFeeCents(String money) {
		if (money == null || money.isBlank()) {
			return 0;
		}
		try {
			double yuan = Double.parseDouble(money.trim());
			return (int) Math.round(yuan * 100.0d);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static Map<String, Object> refundToMap(AftersalesRefund row) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("refund_bn", row.getRefundBn());
		map.put("aftersales_bn", row.getAftersalesBn());
		map.put("order_id", row.getOrderId());
		map.put("company_id", row.getCompanyId());
		map.put("user_id", row.getUserId());
		map.put("refund_status", row.getRefundStatus());
		map.put("refund_fee", row.getRefundFee());
		map.put("pay_type", row.getPayType());
		map.put("currency", row.getCurrency());
		map.put("cur_pay_fee", row.getCurPayFee());
		map.put("create_time", row.getCreateTime());
		return map;
	}

	private static Map<String, Object> aftersalesToMap(Aftersales row) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("aftersales_bn", row.getAftersalesBn());
		map.put("order_id", row.getOrderId());
		map.put("company_id", row.getCompanyId());
		map.put("aftersales_type", row.getAftersalesType());
		map.put("aftersales_status", row.getAftersalesStatus());
		map.put("progress", row.getProgress());
		map.put("reason", row.getReason());
		return map;
	}

	private static Long parseLong(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(trimmed);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
