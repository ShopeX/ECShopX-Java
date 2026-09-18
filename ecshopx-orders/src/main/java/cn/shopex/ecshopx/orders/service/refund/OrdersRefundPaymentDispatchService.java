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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPayChannelExecutor;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrdersRefundPaymentDispatchService {

	private static final DateTimeFormatter HFPAY_DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final List<AftersalesRefundPayChannelExecutor> payChannelExecutors;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final OrdersDepositOrderRefundService ordersDepositOrderRefundService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrdersMapper normalOrdersMapper;

	public OrdersRefundPaymentDispatchService(
			List<AftersalesRefundPayChannelExecutor> payChannelExecutors,
			PointMemberAddPointService pointMemberAddPointService,
			OrdersDepositOrderRefundService ordersDepositOrderRefundService,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			NormalOrdersMapper normalOrdersMapper) {
		this.payChannelExecutors = payChannelExecutors;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.ordersDepositOrderRefundService = ordersDepositOrderRefundService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public Map<String, Object> dispatch(
			long companyId,
			String wxaAppId,
			AftersalesRefund refundData,
			Trade trade,
			int effectiveRefundFee,
			int effectiveRefundPoint,
			boolean resubmit) {
		if (trade == null) {
			throw new ResourceException("支付信息未找到！");
		}
		String payTypeRaw = refundData.getPayType() == null ? "" : refundData.getPayType();
		String payType = payTypeRaw.toLowerCase(Locale.ROOT);
		Map<String, Object> out = new LinkedHashMap<>();
		switch (payType) {
			case "point":
				pointMemberAddPointService.addPointForAftersalesRefund(
						refundData.getUserId(),
						companyId,
						effectiveRefundPoint,
						refundData.getOrderId(),
						refundData.getRefundBn(),
						refundData.getAftersalesBn());
				Map<String, Object> orderProcessLogEntities = new LinkedHashMap<>();
				orderProcessLogEntities.put("order_id", refundData.getOrderId());
				orderProcessLogEntities.put("company_id", companyId);
				orderProcessLogEntities.put("operator_type", "system");
				orderProcessLogEntities.put("remarks", "订单退款");
				orderProcessLogEntities.put(
						"detail",
						"订单号：" + refundData.getOrderId() + "，订单退还积分成功");
				orderProcessLogPublishPort.publish(orderProcessLogEntities);
				out.put("status", "SUCCESS");
				out.put("refund_id", String.valueOf(refundData.getRefundBn()));
				return out;
			case "deposit":
				ordersDepositOrderRefundService.refundOrderDeposit(
						companyId,
						refundData.getUserId(),
						effectiveRefundFee,
						refundData.getOrderId(),
						refundData.getShopId() == null ? 0L : refundData.getShopId(),
						wxaAppId);
				Map<String, Object> depositOrderProcessLogEntities = new LinkedHashMap<>();
				depositOrderProcessLogEntities.put("order_id", refundData.getOrderId());
				depositOrderProcessLogEntities.put("company_id", companyId);
				depositOrderProcessLogEntities.put("operator_type", "system");
				depositOrderProcessLogEntities.put("remarks", "订单退款");
				depositOrderProcessLogEntities.put(
						"detail",
						"订单号：" + refundData.getOrderId() + "，订单退款成功（储值金额渠道)");
				orderProcessLogPublishPort.publish(depositOrderProcessLogEntities);
				out.put("status", "SUCCESS");
				out.put("refund_id", String.valueOf(refundData.getRefundBn()));
				return out;
			case "pos":
			case "offline_pay":
			case "prepaid_point":
				out.put("status", "SUCCESS");
				out.put("refund_id", String.valueOf(refundData.getRefundBn()));
				return out;
			default:
				break;
		}

		int payFee = trade.getPayFee() == null ? 0 : trade.getPayFee();
		String hfOid = refundData.getHfOrderId() == null ? "" : refundData.getHfOrderId().trim();
		HfpayRefundDateHints h = resolveHfpayRefundDateHints(companyId, refundData);
		AftersalesRefundPaymentContext ctx =
				new AftersalesRefundPaymentContext(
						companyId,
						wxaAppId,
						refundData.getRefundBn(),
						refundData.getOrderId(),
						refundData.getUserId(),
						refundData.getShopId() == null ? 0L : refundData.getShopId(),
						refundData.getDistributorId() == null ? 0L : refundData.getDistributorId(),
						refundData.getMerchantId() == null ? 0L : refundData.getMerchantId(),
						refundData.getSupplierId() == null ? 0L : refundData.getSupplierId(),
						payTypeRaw,
						trade.getPayChannel() == null ? "" : trade.getPayChannel(),
						trade.getTradeId(),
						resolveRefundCurrency(refundData, trade),
						payFee,
						effectiveRefundFee,
						trade.getBspayReqDate() == null ? "" : trade.getBspayReqDate(),
						trade.getTransactionId() == null ? "" : trade.getTransactionId(),
						resubmit,
						hfOid,
						h.orgOrderDateYmd,
						h.refundOrderDateYmd,
						h.orderIsProfitsharing);

		for (AftersalesRefundPayChannelExecutor ex : payChannelExecutors) {
			if (ex.supports(payType)) {
				return ex.execute(ctx);
			}
		}
		throw new ResourceException("未知的支付方式");
	}

	private static String resolveRefundCurrency(AftersalesRefund refundData, Trade trade) {
		String currency = refundData.getCurrency();
		if (currency != null && !currency.isBlank()) {
			return currency;
		}
		currency = trade.getFeeType();
		return currency == null || currency.isBlank() ? "CNY" : currency;
	}

	private HfpayRefundDateHints resolveHfpayRefundDateHints(long companyId, AftersalesRefund refundData) {
		String orgYmd = "";
		String refundYmd = "";
		int ps = 0;
		NormalOrders ord =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, refundData.getOrderId())
								.last("LIMIT 1"));
		if (ord != null) {
			orgYmd = epochSecondsToYmd(ord.getCreateTime());
			if (ord.getIsProfitsharing() != null) {
				ps = ord.getIsProfitsharing();
			}
		}
		refundYmd = epochSecondsToYmd(refundData.getCreateTime());
		return new HfpayRefundDateHints(orgYmd, refundYmd, ps);
	}

	private static String epochSecondsToYmd(Integer epochSeconds) {
		if (epochSeconds == null || epochSeconds <= 0) {
			return LocalDate.now(ZoneId.systemDefault()).format(HFPAY_DAY);
		}
		return LocalDate.ofInstant(Instant.ofEpochSecond(epochSeconds.longValue()), ZoneId.systemDefault())
				.format(HFPAY_DAY);
	}

	private record HfpayRefundDateHints(String orgOrderDateYmd, String refundOrderDateYmd, int orderIsProfitsharing) {}
}
