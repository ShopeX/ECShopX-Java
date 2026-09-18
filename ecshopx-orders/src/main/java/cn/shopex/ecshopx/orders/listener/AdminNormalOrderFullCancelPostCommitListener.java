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

package cn.shopex.ecshopx.orders.listener;

import cn.shopex.ecshopx.common.event.SaasErpRefundSpringEvent;
import cn.shopex.ecshopx.common.event.TradeRefundSpringEvent;
import cn.shopex.ecshopx.orders.event.WdtErpTradeCancelSpringEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminNormalOrderFullCancelPostCommitListener {

	private static final Logger log = LoggerFactory.getLogger(AdminNormalOrderFullCancelPostCommitListener.class);

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTradeRefund(TradeRefundSpringEvent event) {
		log.info("TradeRefundSpringEvent orderId={}", event.getPayload().get("order_id"));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSaasErpRefund(SaasErpRefundSpringEvent event) {
		log.info("SaasErpRefundSpringEvent orderId={}", event.getPayload().get("order_id"));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onWdtErpTradeCancel(WdtErpTradeCancelSpringEvent event) {
		log.info("WdtErpTradeCancelSpringEvent orderId={}", event.getPayload().get("order_id"));
	}
}
