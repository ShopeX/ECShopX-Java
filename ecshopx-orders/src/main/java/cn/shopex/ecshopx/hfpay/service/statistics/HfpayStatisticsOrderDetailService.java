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

package cn.shopex.ecshopx.hfpay.service.statistics;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.mapper.HfpayStatisticsOrderDetailMapper;
import cn.shopex.ecshopx.orders.config.OrderAppPayTypeDescHolder;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharing;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharingDetails;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayStatisticsOrderDetailService {

	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final HfpayStatisticsOrderDetailMapper orderDetailMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final TradeMapper tradeMapper;
	private final OrderProfitSharingMapper orderProfitSharingMapper;
	private final OrderProfitSharingDetailsMapper orderProfitSharingDetailsMapper;
	private final OrderAppPayTypeDescHolder orderAppPayTypeDescHolder;

	public HfpayStatisticsOrderDetailService(
			HfpayStatisticsOrderDetailMapper orderDetailMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			TradeMapper tradeMapper,
			OrderProfitSharingMapper orderProfitSharingMapper,
			OrderProfitSharingDetailsMapper orderProfitSharingDetailsMapper,
			OrderAppPayTypeDescHolder orderAppPayTypeDescHolder) {
		this.orderDetailMapper = orderDetailMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.tradeMapper = tradeMapper;
		this.orderProfitSharingMapper = orderProfitSharingMapper;
		this.orderProfitSharingDetailsMapper = orderProfitSharingDetailsMapper;
		this.orderAppPayTypeDescHolder = orderAppPayTypeDescHolder;
	}

	public Map<String, Object> getOrderDetail(long companyId, String orderId) {
		Map<String, Object> res = orderDetailMapper.selectOrderDetailRow(companyId, orderId);
		if (res == null || res.isEmpty()) {
			throw new ResourceException("订单不存在");
		}

		String appPayTypeKey = strOrEmpty(res.get("app_pay_type"));
		String appPayDesc = orderAppPayTypeDescHolder.descForAppPayTypeOrNull(appPayTypeKey);
		res.put("app_pay_type_desc", appPayDesc != null ? appPayDesc : "");

		QueryWrapper<AftersalesRefund> refundListWrapper = new QueryWrapper<>();
		refundListWrapper
				.eq("company_id", companyId)
				.eq("order_id", orderId)
				.notIn("refund_status", "CANCEL");
		List<AftersalesRefund> refundRows = aftersalesRefundMapper.selectList(refundListWrapper);

		String refundStatus = computeRefundStatus(refundRows);
		if (StringUtils.hasText(refundStatus)) {
			res.put("order_status", refundStatus);
		}

		handelOrderStatus(res);

		long refundFeeSum = sumRefundFeeForBalance(companyId, orderId);
		res.put("refund_fee", refundFeeSum);

		long totalFeeLong = toLongAmount(res.get("total_fee"));
		res.put("balance", totalFeeLong - refundFeeSum);

		LambdaQueryWrapper<Trade> tradeW = new LambdaQueryWrapper<>();
		tradeW.eq(Trade::getCompanyId, String.valueOf(companyId))
				.eq(Trade::getOrderId, orderId)
				.eq(Trade::getTradeState, "SUCCESS");
		Trade trade = tradeMapper.selectOne(tradeW);
		if (trade != null) {
			res.put("trade_id", trade.getTradeId() != null ? trade.getTradeId() : "");
			res.put("pay_time", formatPayTime(trade.getTimeExpire()));
		} else {
			res.put("trade_id", "");
			res.put("pay_time", "");
		}

		QueryWrapper<OrderProfitSharing> psW = new QueryWrapper<>();
		psW.eq("company_id", companyId).eq("order_id", orderId);
		OrderProfitSharing ps = orderProfitSharingMapper.selectOne(psW);
		if (ps != null) {
			res.put("hf_order_id", ps.getHfOrderId() != null ? ps.getHfOrderId() : "");
			res.put("hf_order_date", formatPayTime(ps.getHfOrderDate()));
		} else {
			res.put("hf_order_id", "");
			res.put("hf_order_date", "");
		}

		List<Map<String, Object>> refundListArr = buildRefundList(res, refundRows);
		res.put("refund_list", refundListArr);

		QueryWrapper<OrderProfitSharingDetails> detW = new QueryWrapper<>();
		detW.eq("company_id", companyId)
				.eq("order_id", orderId)
				.orderByAsc("created_at")
				.orderByAsc("order_profit_sharing_detail_id");
		List<OrderProfitSharingDetails> details = orderProfitSharingDetailsMapper.selectList(detW);

		String orderDistributorName = res.get("distributor_name") == null
				? ""
				: String.valueOf(res.get("distributor_name"));
		List<Map<String, Object>> shareList = new ArrayList<>();
		for (OrderProfitSharingDetails d : details) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("created_at", formatLocalDateTime(d.getCreatedAt()));
			boolean hq = d.getDistributorId() == null || d.getDistributorId() == 0L;
			row.put("distributor_name", hq ? "总部" : orderDistributorName);
			row.put("total_fee", d.getTotalFee() != null ? d.getTotalFee() : 0);
			shareList.add(row);
		}
		res.put("profit_share_list", shareList);

		normalizeOutputTypes(res);
		return res;
	}

	private static void normalizeOutputTypes(Map<String, Object> res) {
		Object oid = res.get("order_id");
		if (oid != null) {
			res.put("order_id", String.valueOf(oid));
		}
	}

	private static List<Map<String, Object>> buildRefundList(
			Map<String, Object> res, List<AftersalesRefund> refundRows) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (refundRows == null || refundRows.isEmpty()) {
			return out;
		}
		BigDecimal rateBd = toBigDecimalRate(res.get("profitsharing_rate"));
		String distributorName =
				res.get("distributor_name") == null ? "" : String.valueOf(res.get("distributor_name"));

		for (AftersalesRefund v : refundRows) {
			int refundFeeOrig = v.getRefundFee() != null ? v.getRefundFee() : 0;
			BigDecimal refundFeeBd = BigDecimal.valueOf(refundFeeOrig);
			BigDecimal product = refundFeeBd.multiply(rateBd).setScale(0, RoundingMode.DOWN);
			BigDecimal feeAmt = product.divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
			int feeAmtInt = feeAmt.intValueExact();
			BigDecimal shopFeeBd = refundFeeBd.subtract(feeAmt);
			int shopFeeInt = shopFeeBd.intValueExact();

			String refundBnStr = v.getRefundBn() != null ? String.valueOf(v.getRefundBn()) : "";
			String refundIdStr = v.getRefundId() != null ? v.getRefundId() : "";
			String st = v.getRefundStatus() != null ? v.getRefundStatus() : "";

			Map<String, Object> head = new LinkedHashMap<>();
			head.put("refund_bn", refundBnStr);
			head.put("refund_id", refundIdStr);
			head.put("refund_fee", feeAmtInt);
			head.put("refund_status", st);
			head.put("distributor_name", "总部");
			out.add(head);

			Map<String, Object> shop = new LinkedHashMap<>();
			shop.put("refund_bn", refundBnStr);
			shop.put("refund_id", refundIdStr);
			shop.put("refund_fee", shopFeeInt);
			shop.put("refund_status", st);
			shop.put("distributor_name", distributorName);
			out.add(shop);
		}
		return out;
	}

	private long sumRefundFeeForBalance(long companyId, String orderId) {
		QueryWrapper<AftersalesRefund> w = new QueryWrapper<>();
		w.select("IFNULL(SUM(refund_fee), 0) AS s");
		w.eq("company_id", companyId)
				.eq("order_id", orderId)
				.notIn(
						"refund_status",
						"CANCEL",
						"REFUSE",
						"REFUNDCLOSE",
						"CHANGE");
		List<Map<String, Object>> rows = aftersalesRefundMapper.selectMaps(w);
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		Map<String, Object> row = rows.get(0);
		Object v = row.get("s");
		if (v == null && row.size() == 1) {
			v = row.values().iterator().next();
		}
		return toLongAmount(v);
	}

	static String computeRefundStatus(List<AftersalesRefund> refundList) {
		if (refundList == null || refundList.isEmpty()) {
			return "pay";
		}
		Set<String> statuses =
				refundList.stream()
						.map(AftersalesRefund::getRefundStatus)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
		if (statuses.contains("AUDIT_SUCCESS")
				|| statuses.contains("READY")
				|| statuses.contains("PROCESSING")) {
			return "refunding";
		}
		if (statuses.contains("SUCCESS")) {
			return "refundsuccess";
		}
		if (statuses.contains("CHANGE")) {
			return "refundfail";
		}
		if (statuses.contains("REFUSE")
				|| statuses.contains("CANCEL")
				|| statuses.contains("REFUNDCLOSE")) {
			return "pay";
		}
		return "";
	}

	static void handelOrderStatus(Map<String, Object> orderInfo) {
		if (orderInfo == null || orderInfo.isEmpty()) {
			return;
		}
		String orderStatus = strOrEmpty(orderInfo.get("order_status"));
		String cancelStatus = strOrEmpty(orderInfo.get("cancel_status"));
		String deliveryStatus = strOrEmpty(orderInfo.get("delivery_status"));
		String receiptType = strOrEmpty(orderInfo.get("receipt_type"));
		String zitiStatus = strOrEmpty(orderInfo.get("ziti_status"));

		if (("CANCEL_WAIT_PROCESS".equals(orderStatus) && "WAIT_PROCESS".equals(cancelStatus))
				|| ("CANCEL_WAIT_PROCESS".equals(orderStatus) && "NO_APPLY_CANCEL".equals(cancelStatus))
				|| ("PAYED".equals(orderStatus) && "WAIT_PROCESS".equals(cancelStatus))) {
			orderInfo.put("order_status", "refunding");
		} else if (("PAYED".equals(orderStatus)
						&& Set.of("NO_APPLY_CANCEL", "FAILS").contains(cancelStatus))
				|| ("PAYED".equals(orderStatus) && "ziti".equals(receiptType) && "PENDING".equals(zitiStatus))
				|| ("WAIT_BUYER_CONFIRM".equals(orderStatus)
						&& "DONE".equals(deliveryStatus)
						&& "logistics".equals(receiptType))
				|| Set.of("DONE", "REVIEW_PASS").contains(orderStatus)) {
			orderInfo.put("order_status", "pay");
		} else if ("CANCEL".equals(orderStatus) && "SUCCESS".equals(cancelStatus)) {
			orderInfo.put("order_status", "refundsuccess");
		} else if ("DONE".equals(orderStatus) && "FAILS".equals(cancelStatus)) {
			orderInfo.put("order_status", "refundfail");
		}

		orderInfo.remove("delivery_status");
		orderInfo.remove("receipt_type");
		orderInfo.remove("ziti_status");
		orderInfo.remove("cancel_status");
		orderInfo.remove("is_invoiced");
	}

	private static String strOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static long toLongAmount(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return new BigDecimal(String.valueOf(o).trim()).longValue();
		} catch (Exception e) {
			return 0L;
		}
	}

	private static BigDecimal toBigDecimalRate(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		try {
			return new BigDecimal(String.valueOf(o).trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static String formatPayTime(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String s = raw.trim();
		if (s.chars().allMatch(Character::isDigit)) {
			try {
				long sec = Long.parseLong(s);
				if (sec > 1_000_000_000_000L) {
					sec = sec / 1000;
				}
				LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneId.systemDefault());
				return dt.format(DATE_TIME_FMT);
			} catch (Exception e) {
				return "";
			}
		}
		try {
			LocalDateTime dt = LocalDateTime.parse(s, DATE_TIME_FMT);
			return dt.format(DATE_TIME_FMT);
		} catch (DateTimeParseException e1) {
			try {
				LocalDateTime dt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				return dt.format(DATE_TIME_FMT);
			} catch (DateTimeParseException e2) {
				return s;
			}
		}
	}

	private static String formatLocalDateTime(LocalDateTime t) {
		if (t == null) {
			return "";
		}
		return t.format(DATE_TIME_FMT);
	}
}
