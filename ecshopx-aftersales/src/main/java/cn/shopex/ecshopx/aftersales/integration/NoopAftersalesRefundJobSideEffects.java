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

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundJobSideEffects;
import cn.shopex.ecshopx.aftersales.service.TradeInfoSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("noop-aftersales-refund-job-side-effects")
@Slf4j
public class NoopAftersalesRefundJobSideEffects implements AftersalesRefundJobSideEffects {

	@Override
	public void doOnlineRefund(AftersalesRefund refund) {
		log.trace("[noop] doOnlineRefund refundBn={}", refund == null ? null : refund.getRefundBn());
	}

	@Override
	public TradeInfoSnapshot loadTrade(String tradeId, long companyId) {
		log.trace("[noop] loadTrade tradeId={} companyId={}", tradeId, companyId);
		return null;
	}

	@Override
	public void processBargainOrder(long companyId, long orderId) {
		log.trace("[noop] processBargainOrder companyId={} orderId={}", companyId, orderId);
	}

	@Override
	public void notifyTradeRefundSettled(AftersalesRefund refund) {
		log.trace("[noop] notifyTradeRefundSettled refundBn={}", refund == null ? null : refund.getRefundBn());
	}

	@Override
	public AftersalesRefund applyOfflineChannelHintFromOrder(AftersalesRefund refund) {
		log.trace("[noop] applyOfflineChannelHintFromOrder refundBn={}", refund == null ? null : refund.getRefundBn());
		return refund;
	}

	@Override
	public void updateRefundToSuccess(AftersalesRefund refund) {
		log.trace("[noop] updateRefundToSuccess refundBn={}", refund == null ? null : refund.getRefundBn());
	}

	@Override
	public void updateAftersaleFinished(AftersalesRefund refund) {
		log.trace("[noop] updateAftersaleFinished refundBn={}", refund == null ? null : refund.getRefundBn());
	}
}
