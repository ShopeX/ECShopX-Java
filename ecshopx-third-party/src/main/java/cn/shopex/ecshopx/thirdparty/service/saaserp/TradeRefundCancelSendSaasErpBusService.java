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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeRefundCancelSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeRefundCancelSendSaasErpBusService.class);

	private final SaasErpStoreTradeRefundCancelPort saasErpStoreTradeRefundCancelPort;
	private final SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort;

	public TradeRefundCancelSendSaasErpBusService(
			SaasErpStoreTradeRefundCancelPort saasErpStoreTradeRefundCancelPort,
			SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort) {
		this.saasErpStoreTradeRefundCancelPort = saasErpStoreTradeRefundCancelPort;
		this.saasErpCompanySaasBindingReadPort = saasErpCompanySaasBindingReadPort;
	}

	public void handleTradeRefundCancelEntities(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		log.debug("TradeRefundCancelSendSaasErp_entities: {}", entities);
		Long companyId = longObj(entities.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			log.debug("TradeRefundCancelSendSaasErp_skip reason=missing_company_id");
			return;
		}
		long orderId = longVal(entities.get("order_id"));
		if (orderId <= 0L) {
			log.debug("TradeRefundCancelSendSaasErp_skip reason=missing_order_id companyId={}", companyId);
			return;
		}
		if (!hasRefundBn(entities.get("refund_bn"))) {
			log.debug(
					"TradeRefundCancelSendSaasErp_skip reason=missing_refund_bn companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		if (!saasErpCompanySaasBindingReadPort.isSaasErpOutboundEnabled(companyId)) {
			log.debug(
					"TradeRefundCancelSendSaasErp_skip reason=saas_erp_outbound_disabled companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		Map<String, Object> body = new LinkedHashMap<>(entities);
		try {
			saasErpStoreTradeRefundCancelPort.callStoreTradeRefundCancel(body);
		} catch (RuntimeException e) {
			log.debug(
					"store.trade.refund.cancel failed companyId={} orderId={} msg={}",
					companyId,
					orderId,
					e.getMessage());
		}
	}

	private static boolean hasRefundBn(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return StringUtils.hasText(s);
		}
	}

	private static Long longObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longVal(Object o) {
		Long v = longObj(o);
		return v != null ? v : 0L;
	}
}
