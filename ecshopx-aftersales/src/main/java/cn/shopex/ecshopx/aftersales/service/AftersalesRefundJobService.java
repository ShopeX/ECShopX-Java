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

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundQueueMessage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 退款队列消息的同步编排入口，可由统一调度出队或单测直接调用。复杂写库/外呼由 {@link
 * AftersalesRefundJobSideEffects} 承载。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AftersalesRefundJobService {

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AftersalesRefundJobSideEffects sideEffects;

	/**
	 * 由消息中 <code>refund_bn+company_id</code> 读库并跑完分支；<code>order_id</code> 为日志等辅助字段。
	 */
	public boolean handle(AftersalesRefundQueueMessage message) {
		AftersalesRefund r = loadByKeys(message);
		if (r == null || !"AUDIT_SUCCESS".equals(r.getRefundStatus())) {
			return false;
		}
		String payType = r.getPayType() == null ? "" : r.getPayType();
		if ("offline_pay".equalsIgnoreCase(payType)) {
			log.info(
					"schedule_refund::refundJob: offline_pay skip online refund company_id={} order_id={} refund_bn={}",
					r.getCompanyId(),
					r.getOrderId(),
					r.getRefundBn());
			return true;
		}
		if (r.getAftersalesBn() == null || r.getAftersalesBn() == 0L) {
			// PHP: doRefund only (TradeRefundFinish fired inside); no separate notify
			sideEffects.doOnlineRefund(r);
			TradeInfoSnapshot trade =
					sideEffects.loadTrade(safeStr(r.getTradeId()), r.getCompanyId() == null ? 0L : r.getCompanyId());
			if (trade != null && "bargain".equalsIgnoreCase(safeStr(trade.tradeSourceType()))) {
				sideEffects.processBargainOrder(
						r.getCompanyId() == null ? 0L : r.getCompanyId(),
						r.getOrderId() == null ? 0L : r.getOrderId());
			}
			return true;
		}
		AftersalesRefund patched = sideEffects.applyOfflineChannelHintFromOrder(r);
		if ("offline".equalsIgnoreCase(safeStr(patched.getRefundChannel()))) {
			// PHP: update SUCCESS only — no TradeRefundFinish in RefundJob
			sideEffects.updateRefundToSuccess(patched);
		} else if ("original".equalsIgnoreCase(safeStr(patched.getRefundChannel()))) {
			// PHP: doRefund only (Finish inside); no separate notify
			sideEffects.doOnlineRefund(patched);
		}
		sideEffects.updateAftersaleFinished(patched);
		return true;
	}

	private AftersalesRefund loadByKeys(AftersalesRefundQueueMessage message) {
		if (message.getRefundBn() == null
				|| message.getCompanyId() == null) {
			return null;
		}
		return aftersalesRefundMapper.selectOne(
				new QueryWrapper<AftersalesRefund>()
						.eq("refund_bn", message.getRefundBn())
						.eq("company_id", message.getCompanyId()));
	}

	private static String safeStr(String s) {
		return s == null ? "" : s.trim();
	}
}
