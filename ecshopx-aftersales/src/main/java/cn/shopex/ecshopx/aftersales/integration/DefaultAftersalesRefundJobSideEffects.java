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

package cn.shopex.ecshopx.aftersales.integration;

import cn.shopex.ecshopx.aftersales.dispatch.AftersalesRefundTradeRefundFinishPayloadMapper;
import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundJobSideEffects;
import cn.shopex.ecshopx.aftersales.service.TradeInfoSnapshot;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundOnlineRefundPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.TradeByIdReadPort;
import cn.shopex.ecshopx.common.port.promotions.BargainOrderActivityStatusPort;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class DefaultAftersalesRefundJobSideEffects implements AftersalesRefundJobSideEffects {

	private final AftersalesRefundOnlineRefundPort aftersalesRefundOnlineRefundPort;
	private final TradeRefundFinishEventDispatchPublisher tradeRefundFinishEventDispatchPublisher;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final TradeByIdReadPort tradeByIdReadPort;
	private final BargainOrderActivityStatusPort bargainOrderActivityStatusPort;

	@Override
	public void doOnlineRefund(AftersalesRefund refund) {
		if (refund == null || refund.getCompanyId() == null || refund.getRefundBn() == null) {
			log.warn("doOnlineRefund skipped: missing companyId or refundBn");
			return;
		}
		aftersalesRefundOnlineRefundPort.executeRefund(refund.getCompanyId(), refund.getRefundBn());
	}

	/** PHP {@code TradeService::getInfoById} → {@code trade_source_type}. */
	@Override
	public TradeInfoSnapshot loadTrade(String tradeId, long companyId) {
		if (!StringUtils.hasText(tradeId)) {
			return null;
		}
		Optional<Map<String, Object>> row = tradeByIdReadPort.getByTradeId(tradeId.trim(), companyId);
		if (row.isEmpty()) {
			return null;
		}
		Map<String, Object> m = row.get();
		String tid = m.get("trade_id") == null ? tradeId : String.valueOf(m.get("trade_id"));
		String source =
				m.get("trade_source_type") == null ? "" : String.valueOf(m.get("trade_source_type")).trim();
		return new TradeInfoSnapshot(tid, source);
	}

	/**
	 * PHP {@code RefundJob::__processBargainOrder}：订单 {@code order_class=bargain} 时以 state=0
	 * 回退助力活动状态。
	 */
	@Override
	public void processBargainOrder(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		Optional<Map<String, Object>> header = orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (header.isEmpty()) {
			return;
		}
		Map<String, Object> order = header.get();
		String orderClass =
				order.get("order_class") == null ? "" : String.valueOf(order.get("order_class")).trim();
		if (!"bargain".equalsIgnoreCase(orderClass)) {
			return;
		}
		long userId = longVal(order.get("user_id"));
		long bargainId = longVal(order.get("act_id"));
		bargainOrderActivityStatusPort.changeOrderActivityStatus(userId, bargainId, 0);
	}

	@Override
	public void notifyTradeRefundSettled(AftersalesRefund refund) {
		if (refund == null) {
			return;
		}
		log.trace("notifyTradeRefundSettled refundBn={}", refund.getRefundBn());
		tradeRefundFinishEventDispatchPublisher.publish(
				AftersalesRefundTradeRefundFinishPayloadMapper.toPayload(refund));
	}

	/**
	 * PHP {@code RefundJob} 售后退款：当 {@code refund_channel != offline} 时读主单；若 {@code pay_type ==
	 * offline}，将内存中的 {@code refund_channel} 置为该值（不落库）。
	 */
	@Override
	public AftersalesRefund applyOfflineChannelHintFromOrder(AftersalesRefund refund) {
		if (refund == null) {
			return null;
		}
		String channel = refund.getRefundChannel() == null ? "" : refund.getRefundChannel().trim();
		if ("offline".equalsIgnoreCase(channel)) {
			return refund;
		}
		Long companyId = refund.getCompanyId();
		Long orderId = refund.getOrderId();
		if (companyId == null || orderId == null || orderId == 0L) {
			return refund;
		}
		Optional<Map<String, Object>> header = orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (header.isEmpty()) {
			return refund;
		}
		Object payTypeObj = header.get().get("pay_type");
		String payType = payTypeObj == null ? "" : String.valueOf(payTypeObj).trim();
		if ("offline".equals(payType)) {
			refund.setRefundChannel(payType);
			log.info(
					"applyOfflineChannelHintFromOrder refundBn={} orderId={} refund_channel→offline",
					refund.getRefundBn(),
					orderId);
		}
		return refund;
	}

	/**
	 * PHP {@code RefundJob} 线下渠道：{@code updateOneBy(filter, ['refund_status' => 'SUCCESS'])}.
	 */
	@Override
	public void updateRefundToSuccess(AftersalesRefund refund) {
		if (refund == null || refund.getCompanyId() == null || refund.getRefundBn() == null) {
			log.trace(
					"updateRefundToSuccess skipped refundBn={}",
					refund == null ? null : refund.getRefundBn());
			return;
		}
		log.info(
				"updateRefundToSuccess companyId={} refundBn={}",
				refund.getCompanyId(),
				refund.getRefundBn());
		aftersalesRefundMapper.update(
				null,
				new LambdaUpdateWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, refund.getCompanyId())
						.eq(AftersalesRefund::getRefundBn, refund.getRefundBn())
						.set(AftersalesRefund::getRefundStatus, "SUCCESS"));
	}

	/**
	 * PHP {@code RefundJob::__updateAfterSaleFinish}: set aftersales / aftersales_detail to finished
	 * ({@code aftersales_status=2}, {@code progress=4}).
	 */
	@Override
	public void updateAftersaleFinished(AftersalesRefund refund) {
		if (refund == null
				|| refund.getCompanyId() == null
				|| refund.getAftersalesBn() == null
				|| refund.getAftersalesBn() == 0L) {
			log.trace(
					"updateAftersaleFinished skipped refundBn={}",
					refund == null ? null : refund.getRefundBn());
			return;
		}
		long companyId = refund.getCompanyId();
		long aftersalesBn = refund.getAftersalesBn();
		int now = (int) (System.currentTimeMillis() / 1000L);
		log.info(
				"updateAftersaleFinished companyId={} aftersalesBn={} refundBn={}",
				companyId,
				aftersalesBn,
				refund.getRefundBn());
		aftersalesMapper.update(
				null,
				new LambdaUpdateWrapper<Aftersales>()
						.eq(Aftersales::getCompanyId, companyId)
						.eq(Aftersales::getAftersalesBn, aftersalesBn)
						.set(Aftersales::getAftersalesStatus, 2)
						.set(Aftersales::getProgress, 4)
						.set(Aftersales::getUpdateTime, now));
		aftersalesDetailMapper.update(
				null,
				new LambdaUpdateWrapper<AftersalesDetail>()
						.eq(AftersalesDetail::getCompanyId, companyId)
						.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
						.set(AftersalesDetail::getAftersalesStatus, 2)
						.set(AftersalesDetail::getProgress, 4)
						.set(AftersalesDetail::getUpdateTime, now));
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
}
