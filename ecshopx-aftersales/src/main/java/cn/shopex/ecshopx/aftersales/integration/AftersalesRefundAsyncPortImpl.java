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

import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.common.dispatch.AftersalesSuccessSendMsgJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrderRefundCompleteJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleCancelNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitConfirmNoticeJobDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AftersalesRefundAsyncPortImpl implements AftersalesRefundAsyncPort {

	private static final Logger log = LoggerFactory.getLogger(AftersalesRefundAsyncPortImpl.class);

	private final OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher;
	private final InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher;
	private final AftersalesSuccessSendMsgJobDispatchPublisher aftersalesSuccessSendMsgJobDispatchPublisher;
	private final SendAfterSaleWaitConfirmNoticeJobDispatchPublisher sendAfterSaleWaitConfirmNoticeJobDispatchPublisher;
	private final SendAfterSaleCancelNoticeJobDispatchPublisher sendAfterSaleCancelNoticeJobDispatchPublisher;

	public AftersalesRefundAsyncPortImpl(
			OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher,
			InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher,
			AftersalesSuccessSendMsgJobDispatchPublisher aftersalesSuccessSendMsgJobDispatchPublisher,
			SendAfterSaleWaitConfirmNoticeJobDispatchPublisher sendAfterSaleWaitConfirmNoticeJobDispatchPublisher,
			SendAfterSaleCancelNoticeJobDispatchPublisher sendAfterSaleCancelNoticeJobDispatchPublisher) {
		this.orderRefundCompleteJobDispatchPublisher = orderRefundCompleteJobDispatchPublisher;
		this.invoiceRedJobDispatchPublisher = invoiceRedJobDispatchPublisher;
		this.aftersalesSuccessSendMsgJobDispatchPublisher = aftersalesSuccessSendMsgJobDispatchPublisher;
		this.sendAfterSaleWaitConfirmNoticeJobDispatchPublisher = sendAfterSaleWaitConfirmNoticeJobDispatchPublisher;
		this.sendAfterSaleCancelNoticeJobDispatchPublisher = sendAfterSaleCancelNoticeJobDispatchPublisher;
	}

	@Override
	public void scheduleOrderRefundComplete(long companyId, long orderId) {
		orderRefundCompleteJobDispatchPublisher.publish(companyId, orderId);
	}

	@Override
	public void scheduleInvoiceRed(Map<String, Object> aftersalesResultRow) {
		if (aftersalesResultRow == null) {
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>(aftersalesResultRow);
		invoiceRedJobDispatchPublisher.publish(payload);
	}

	@Override
	public void scheduleAftersalesSuccessSendMsg(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Long companyId = coerceLong(payload.get("company_id"));
		Long orderId = coerceLong(payload.get("order_id"));
		Long aftersalesBn = coerceLong(payload.get("aftersales_bn"));
		if (companyId == null || orderId == null || aftersalesBn == null) {
			log.warn(
					"scheduleAftersalesSuccessSendMsg skipped: invalid ids company_id={} order_id={} aftersales_bn={}",
					payload.get("company_id"),
					payload.get("order_id"),
					payload.get("aftersales_bn"));
			return;
		}
		aftersalesSuccessSendMsgJobDispatchPublisher.publish(companyId, orderId, aftersalesBn);
	}

	private static Long coerceLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	@Override
	public void scheduleSendAfterSaleWaitConfirmNotice(long companyId, long aftersalesBn) {
		sendAfterSaleWaitConfirmNoticeJobDispatchPublisher.publish(companyId, aftersalesBn);
	}

	@Override
	public void scheduleSendAftersaleCancelNotice(long companyId, long aftersalesBn) {
		sendAfterSaleCancelNoticeJobDispatchPublisher.publish(companyId, aftersalesBn);
	}
}
