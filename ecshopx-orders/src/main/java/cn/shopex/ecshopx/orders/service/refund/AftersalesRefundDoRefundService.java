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

import cn.shopex.ecshopx.aftersales.dispatch.AftersalesRefundTradeRefundFinishPayloadMapper;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.EmployeePurchasePrepaidAftersalesRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.common.event.TradeRefundSpringEvent;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesRefundDoRefundService {

	private final OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final TradeMapper tradeMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator;
	private final TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher;
	private final RefundErrorLogsRecorder refundErrorLogsRecorder;
	private final ObjectProvider<EmployeePurchasePrepaidAftersalesRestorePort>
			employeePurchasePrepaidAftersalesRestorePort;
	private final EmployeePurchasePrepaidRefundLinesResolver employeePurchasePrepaidRefundLinesResolver;

	public AftersalesRefundDoRefundService(
			OrdersRefundPaymentDispatchService ordersRefundPaymentDispatchService,
			AftersalesRefundMapper aftersalesRefundMapper,
			TradeMapper tradeMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			ApplicationEventPublisher applicationEventPublisher,
			PointMemberAddPointService pointMemberAddPointService,
			TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator,
			TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher,
			RefundErrorLogsRecorder refundErrorLogsRecorder,
			ObjectProvider<EmployeePurchasePrepaidAftersalesRestorePort>
					employeePurchasePrepaidAftersalesRestorePort,
			EmployeePurchasePrepaidRefundLinesResolver employeePurchasePrepaidRefundLinesResolver) {
		this.ordersRefundPaymentDispatchService = ordersRefundPaymentDispatchService;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.tradeMapper = tradeMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.applicationEventPublisher = applicationEventPublisher;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.tradeRefundStatisticsJobDispatchPublisher = tradeRefundStatisticsJobDispatchPublisher;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.hfPayOrderApplyIdGenerator = hfPayOrderApplyIdGenerator;
		this.tradeRefundFinishEventDispatchPublisher = tradeRefundFinishEventDispatchPublisher;
		this.refundErrorLogsRecorder = refundErrorLogsRecorder;
		this.employeePurchasePrepaidAftersalesRestorePort = employeePurchasePrepaidAftersalesRestorePort;
		this.employeePurchasePrepaidRefundLinesResolver = employeePurchasePrepaidRefundLinesResolver;
	}

	/**
	 * Dep-graph {@code rdeps(..., doRefund)}: {@code cn.shopex.ecshopx.orders.service.refund.RefundErrorLogsService},
	 * {@code cn.shopex.ecshopx.orders.integration.AftersalesRefundOnlineRefundPortImpl}; URL filter hits
	 * {@code GET /api/v1/trade/refunderrorlogs/list}, {@code PUT /api/v1/trade/refunderrorlogs/resubmit/{id}}.
	 */
	public void doRefund(long companyId, long refundBn, boolean resubmit) {
		AftersalesRefund refund =
				aftersalesRefundMapper.selectOne(
						new LambdaQueryWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.eq(AftersalesRefund::getRefundBn, refundBn)
								.last("LIMIT 1"));
		if (refund == null) {
			throw new ResourceException("退款单不存在");
		}

		String freightType = refund.getFreightType() == null ? "cash" : refund.getFreightType();
		int baseRefundFee = refund.getRefundFee() == null ? 0 : refund.getRefundFee();
		int baseRefundPoint = refund.getRefundPoint() == null ? 0 : refund.getRefundPoint();
		int freight = refund.getFreight() == null ? 0 : refund.getFreight();
		int effectiveRefundFee = baseRefundFee;
		int effectiveRefundPoint = baseRefundPoint;
		if ("cash".equals(freightType)) {
			effectiveRefundFee += freight;
		} else if ("point".equals(freightType)) {
			effectiveRefundPoint += freight;
		}

		if (refund.getTradeId() == null || refund.getTradeId().isBlank()) {
			throw new ResourceException("支付信息未找到！");
		}
		Trade trade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getTradeId, refund.getTradeId())
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 1"));
		if (trade == null) {
			throw new ResourceException("支付信息未找到！");
		}

		String payType = refund.getPayType() == null ? "" : refund.getPayType().toLowerCase(Locale.ROOT);
		String wxa = trade.getWxaAppid() == null ? "" : trade.getWxaAppid();
		Map<String, Object> refundDataPayload =
				buildRefundDataPayload(refund, trade, effectiveRefundFee, effectiveRefundPoint, companyId);

		Map<String, Object> payRes;
		if ("prepaid_point".equals(payType)
				|| (!"point".equals(payType) && effectiveRefundFee <= 0)) {
			payRes = new LinkedHashMap<>();
			payRes.put("status", "SUCCESS");
			payRes.put("refund_id", "");
		} else {
			if ("localpay".equals(payType)) {
				throw new ResourceException("0元订单不支持退款");
			}
			if ("hfpay".equals(payType) && !StringUtils.hasText(refund.getHfOrderId())) {
				String hfOid = hfPayOrderApplyIdGenerator.nextOrderId();
				aftersalesRefundMapper.update(
						null,
						new LambdaUpdateWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.eq(AftersalesRefund::getRefundBn, refundBn)
								.set(AftersalesRefund::getHfOrderId, hfOid));
				refund.setHfOrderId(hfOid);
			}
			try {
				payRes =
						ordersRefundPaymentDispatchService.dispatch(
								companyId,
								wxa,
								refund,
								trade,
								effectiveRefundFee,
								effectiveRefundPoint,
								resubmit);
				if ("FAIL".equals(String.valueOf(payRes.getOrDefault("status", "")))) {
					refundErrorLogsRecorder.saveRefundError(companyId, wxa, refundDataPayload, payRes);
				}
			} catch (Exception e) {
				Map<String, Object> failResult = new LinkedHashMap<>();
				failResult.put("status", "FAIL");
				failResult.put("error_code", "0");
				failResult.put("error_desc", e.getMessage());
				refundErrorLogsRecorder.saveRefundError(companyId, wxa, refundDataPayload, failResult);
				payRes = failResult;
			}
		}

		if ("adapay".equals(payType)
				&& !Boolean.TRUE.equals(
						payRes.get("order_process_log_via_payment_reverse"))) {
			publishAdapayRefundOrderProcessLog(companyId, refund, payRes);
		}

		if (!"point".equals(payType)
				&& !"prepaid_point".equals(payType)
				&& effectiveRefundPoint > 0
				&& !resubmit) {
			pointMemberAddPointService.addPointForAftersalesRefund(
					refund.getUserId(),
					companyId,
					effectiveRefundPoint,
					refund.getOrderId(),
					refund.getRefundBn(),
					refund.getAftersalesBn());
		}

		String payStatus = String.valueOf(payRes.getOrDefault("status", ""));
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<AftersalesRefund> u =
				new LambdaUpdateWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getRefundBn, refundBn);
		if ("SUCCESS".equals(payStatus) || "PROCESSING".equals(payStatus)) {
			String refundId = payRes.get("refund_id") == null ? "" : String.valueOf(payRes.get("refund_id"));
			u.set(AftersalesRefund::getRefundId, refundId)
					.set(AftersalesRefund::getRefundStatus, "SUCCESS")
					.set(AftersalesRefund::getRefundedFee, effectiveRefundFee)
					.set(AftersalesRefund::getRefundedPoint, effectiveRefundPoint)
					.set(AftersalesRefund::getRefundSuccessTime, (long) now)
					.set(AftersalesRefund::getUpdateTime, now);
		} else {
			u.set(AftersalesRefund::getRefundStatus, "CHANGE").set(AftersalesRefund::getUpdateTime, now);
		}
		int updated = aftersalesRefundMapper.update(null, u);
		if (updated <= 0) {
			throw new ResourceException("退款单状态更新失败");
		}

		AftersalesRefund fresh =
				aftersalesRefundMapper.selectOne(
						new LambdaQueryWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.eq(AftersalesRefund::getRefundBn, refundBn)
								.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException("退款单不存在");
		}
		if ("SUCCESS".equals(fresh.getRefundStatus())) {
			updateRefundedFee(fresh);
			restoreEmployeePurchasePrepaidIfNeeded(
					companyId, fresh, effectiveRefundFee, effectiveRefundPoint);
		}
		tradeRefundFinishEventDispatchPublisher.publish(
				AftersalesRefundTradeRefundFinishPayloadMapper.toPayload(fresh));
		if ("SUCCESS".equals(payStatus) && "SUCCESS".equals(fresh.getRefundStatus())) {
			Map<String, Object> statsPayload = new LinkedHashMap<>();
			statsPayload.put("company_id", companyId);
			statsPayload.put("order_id", fresh.getOrderId());
			statsPayload.put("refund_bn", fresh.getRefundBn());
			statsPayload.put("trade_id", fresh.getTradeId());
			statsPayload.put("pay_type", fresh.getPayType());
			statsPayload.put("refund_fee", fresh.getRefundFee() == null ? 0 : fresh.getRefundFee());
			statsPayload.put("pay_fee", trade.getPayFee() == null ? 0 : trade.getPayFee());
			if (trade.getDistributorId() != null && !trade.getDistributorId().isBlank()) {
				statsPayload.put("distributor_id", trade.getDistributorId());
			}
			if (trade.getMerchantId() != null && trade.getMerchantId() > 0L) {
				statsPayload.put("merchant_id", trade.getMerchantId());
			}
			tradeRefundStatisticsJobDispatchPublisher.publish(statsPayload);
		}
		applicationEventPublisher.publishEvent(
				new TradeRefundSpringEvent(this, refundEntityToEventPayload(fresh)));
	}

	private void restoreEmployeePurchasePrepaidIfNeeded(
			long companyId,
			AftersalesRefund refund,
			int effectiveRefundFee,
			int effectiveRefundPoint) {
		String payType = refund.getPayType() == null ? "" : refund.getPayType().toLowerCase(Locale.ROOT);
		if (!"prepaid_point".equals(payType)) {
			return;
		}
		EmployeePurchasePrepaidAftersalesRestorePort port =
				employeePurchasePrepaidAftersalesRestorePort.getIfAvailable();
		if (port == null) {
			return;
		}
		int requested = Math.max(0, effectiveRefundFee) + Math.max(0, effectiveRefundPoint);
		port.restoreOnRefundSuccess(
				companyId,
				refund.getOrderId(),
				refund.getRefundBn(),
				requested,
				employeePurchasePrepaidRefundLinesResolver.resolve(companyId, refund));
	}

	/**
	 * Dep-graph {@code rdeps(..., publishAdapayRefundOrderProcessLog)}: no URL matches; only
	 * {@link #doRefund(long, long, boolean)} calls this. Entry callers of that method:
	 * {@code cn.shopex.ecshopx.orders.service.refund.RefundErrorLogsService},
	 * {@code cn.shopex.ecshopx.orders.integration.AftersalesRefundOnlineRefundPortImpl}.
	 */
	private void publishAdapayRefundOrderProcessLog(
			long companyId, AftersalesRefund refund, Map<String, Object> payRes) {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", refund.getOrderId());
		entities.put("company_id", companyId);
		entities.put("operator_type", "system");
		entities.put("remarks", "订单退款");
		String payStatus = String.valueOf(payRes.getOrDefault("status", ""));
		long orderId = refund.getOrderId();
		if ("SUCCESS".equals(payStatus) || "PROCESSING".equals(payStatus)) {
			entities.put(
					"detail",
					"订单号：" + orderId + "，订单退款成功（adapay支付渠道）");
		} else {
			Object errObj = payRes.get("error_msg");
			if (errObj == null) {
				errObj = payRes.get("error_desc");
			}
			String errorMsg = errObj == null ? "" : String.valueOf(errObj);
			entities.put(
					"detail",
					"订单号：" + orderId + "，订单退款失败（adapay支付渠道），失败原因：" + errorMsg);
		}
		orderProcessLogPublishPort.publish(entities);
	}

	private void updateRefundedFee(AftersalesRefund refund) {
		Long aftersalesBn = refund.getAftersalesBn();
		if (aftersalesBn == null || aftersalesBn == 0L) {
			long orderId = refund.getOrderId();
			long companyId = refund.getCompanyId();
			List<NormalOrdersItems> items =
					normalOrdersItemsMapper.selectList(
							new LambdaQueryWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getCompanyId, companyId)
									.eq(NormalOrdersItems::getOrderId, orderId));
			for (NormalOrdersItems row : items) {
				Integer totalFee = row.getTotalFee();
				if (totalFee == null) {
					continue;
				}
				LambdaUpdateWrapper<NormalOrdersItems> iu =
						new LambdaUpdateWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getId, row.getId())
								.set(NormalOrdersItems::getRefundedFee, totalFee);
				normalOrdersItemsMapper.update(null, iu);
			}
			return;
		}

		long companyId = refund.getCompanyId();
		List<AftersalesDetail> details =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
		int refundedFeeTotal = refund.getRefundedFee() == null ? 0 : refund.getRefundedFee();
		int refundFeeTotal = refund.getRefundFee() == null ? 0 : refund.getRefundFee();
		int totalAllocated = 0;
		for (int i = 0; i < details.size(); i++) {
			AftersalesDetail row = details.get(i);
			int piece;
			if (i == details.size() - 1) {
				piece = refundedFeeTotal - totalAllocated;
			} else {
				piece = 0;
				if (refundFeeTotal > 0 && row.getRefundFee() != null) {
					BigDecimal ratio =
							BigDecimal.valueOf(refundedFeeTotal)
									.divide(BigDecimal.valueOf(refundFeeTotal), 10, RoundingMode.HALF_UP);
					piece =
							ratio
									.multiply(BigDecimal.valueOf(row.getRefundFee()))
									.setScale(0, RoundingMode.HALF_UP)
									.intValue();
					totalAllocated += piece;
				}
			}
			if (piece > 0 && row.getSubOrderId() != null) {
				NormalOrdersItems orderItem =
						normalOrdersItemsMapper.selectById(row.getSubOrderId());
				if (orderItem != null) {
					int existing = orderItem.getRefundedFee() == null ? 0 : orderItem.getRefundedFee();
					LambdaUpdateWrapper<NormalOrdersItems> iu =
							new LambdaUpdateWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getId, orderItem.getId())
									.set(NormalOrdersItems::getRefundedFee, existing + piece);
					normalOrdersItemsMapper.update(null, iu);
				}
			}
		}
	}

	private static Map<String, Object> buildRefundDataPayload(
			AftersalesRefund refund,
			Trade trade,
			int effectiveRefundFee,
			int effectiveRefundPoint,
			long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("refund_bn", refund.getRefundBn());
		m.put("order_id", refund.getOrderId());
		m.put("trade_id", refund.getTradeId());
		m.put("pay_type", refund.getPayType());
		m.put("refund_fee", effectiveRefundFee);
		m.put("refund_point", effectiveRefundPoint);
		m.put("pay_fee", trade.getPayFee() == null ? 0 : trade.getPayFee());
		if (refund.getDistributorId() != null) {
			m.put("distributor_id", refund.getDistributorId());
		}
		if (refund.getMerchantId() != null) {
			m.put("merchant_id", refund.getMerchantId());
		}
		if (refund.getSupplierId() != null) {
			m.put("supplier_id", refund.getSupplierId());
		}
		if (trade.getBspayReqDate() != null && !trade.getBspayReqDate().isBlank()) {
			m.put("bspay_req_date", trade.getBspayReqDate());
		}
		if (trade.getTransactionId() != null && !trade.getTransactionId().isBlank()) {
			m.put("transaction_id", trade.getTransactionId());
		}
		return m;
	}

	private static Map<String, Object> refundEntityToEventPayload(AftersalesRefund r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("refund_bn", r.getRefundBn());
		m.put("aftersales_bn", r.getAftersalesBn());
		m.put("order_id", r.getOrderId());
		m.put("trade_id", r.getTradeId());
		m.put("company_id", r.getCompanyId());
		m.put("user_id", r.getUserId());
		m.put("refund_status", r.getRefundStatus());
		m.put("refund_fee", r.getRefundFee());
		m.put("refunded_fee", r.getRefundedFee());
		m.put("refund_point", r.getRefundPoint());
		m.put("refunded_point", r.getRefundedPoint());
		m.put("pay_type", r.getPayType());
		m.put("refund_id", r.getRefundId());
		m.put("refund_success_time", r.getRefundSuccessTime());
		m.put("create_time", r.getCreateTime());
		m.put("update_time", r.getUpdateTime());
		return m;
	}
}
