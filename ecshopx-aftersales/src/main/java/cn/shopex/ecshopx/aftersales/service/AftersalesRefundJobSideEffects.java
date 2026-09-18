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

/**
 * 慢队列消费端与第三方/多表写之间的衔接；便于单测以 mock 对位 analysis §3.2 各子路径。
 */
public interface AftersalesRefundJobSideEffects {

	/** 售前路径：对位 doRefund 及其内部线上支付/更新等 */
	void doOnlineRefund(AftersalesRefund refund);

	/** 对位 <code>TradeService::getInfoById</code>，与订单侧 trade 源类型判断 */
	TradeInfoSnapshot loadTrade(String tradeId, long companyId);

	/** 对位 <code>__processBargainOrder</code> */
	void processBargainOrder(long companyId, long orderId);

	/** 对位 doRefund 成功后的 <code>TradeRefundFinish</code> 外发链（营销中心/DM 等） */
	void notifyTradeRefundSettled(AftersalesRefund refund);

	/** 6-1：按主单可能把 <code>refund_channel</code> 置为 offline；若无需改则原样返回 */
	AftersalesRefund applyOfflineChannelHintFromOrder(AftersalesRefund refund);

	/** 6-2：仅将退款单置为成功态 */
	void updateRefundToSuccess(AftersalesRefund refund);

	/** 6-4：对位 <code>__updateAfterSaleFinish</code>，写 <code>aftersales</code> / <code>aftersales_detail</code> */
	void updateAftersaleFinished(AftersalesRefund refund);
}
