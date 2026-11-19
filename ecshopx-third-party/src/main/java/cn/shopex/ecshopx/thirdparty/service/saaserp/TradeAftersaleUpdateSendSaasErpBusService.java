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
public class TradeAftersaleUpdateSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeAftersaleUpdateSendSaasErpBusService.class);

	private final SaasErpStoreTradeRefundAddPort saasErpStoreTradeRefundAddPort;
	private final SaasErpStoreTradeAftersaleStatusUpdatePort saasErpStoreTradeAftersaleStatusUpdatePort;
	private final SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort;
	private final SaasErpTradeAftersaleOrderStructPort saasErpTradeAftersaleOrderStructPort;

	public TradeAftersaleUpdateSendSaasErpBusService(
			SaasErpStoreTradeRefundAddPort saasErpStoreTradeRefundAddPort,
			SaasErpStoreTradeAftersaleStatusUpdatePort saasErpStoreTradeAftersaleStatusUpdatePort,
			SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort,
			SaasErpTradeAftersaleOrderStructPort saasErpTradeAftersaleOrderStructPort) {
		this.saasErpStoreTradeRefundAddPort = saasErpStoreTradeRefundAddPort;
		this.saasErpStoreTradeAftersaleStatusUpdatePort = saasErpStoreTradeAftersaleStatusUpdatePort;
		this.saasErpCompanySaasBindingReadPort = saasErpCompanySaasBindingReadPort;
		this.saasErpTradeAftersaleOrderStructPort = saasErpTradeAftersaleOrderStructPort;
	}

	public void handleTradeAftersalesUpdateEntities(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		log.debug("TradeAftersaleUpdateSendSaasErp_entities: {}", entities);
		Long companyId = longObj(entities.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			log.debug("TradeAftersaleUpdateSendSaasErp_skip reason=missing_company_id");
			return;
		}
		long orderId = longVal(entities.get("order_id"));
		if (orderId <= 0L) {
			log.debug("TradeAftersaleUpdateSendSaasErp_skip reason=missing_order_id companyId={}", companyId);
			return;
		}
		Object aftersalesBnRaw = entities.get("aftersales_bn");
		if (!hasAftersalesBn(aftersalesBnRaw)) {
			log.debug(
					"TradeAftersaleUpdateSendSaasErp_skip reason=missing_aftersales_bn companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		if (!saasErpCompanySaasBindingReadPort.isSaasErpOutboundEnabled(companyId)) {
			log.debug(
					"TradeAftersaleUpdateSendSaasErp_skip reason=saas_erp_outbound_disabled companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		String type = text(entities.get("aftersales_type"));
		try {
			Map<String, Object> outboundBody = new LinkedHashMap<>(entities);
			Map<String, Object> sourced =
					saasErpTradeAftersaleOrderStructPort.loadOrderAftersalePayloadOrNull(
							companyId, orderId, aftersalesBnRaw);
			if (sourced != null && !sourced.isEmpty()) {
				outboundBody.putAll(sourced);
			}
			if ("ONLY_REFUND".equalsIgnoreCase(type)) {
				saasErpStoreTradeRefundAddPort.callStoreTradeRefundAdd(companyId, outboundBody);
			} else {
				saasErpStoreTradeAftersaleStatusUpdatePort.callStoreTradeAftersaleStatusUpdate(
						companyId, outboundBody);
			}
		} catch (RuntimeException e) {
			log.debug(
					"TradeAftersaleUpdateSendSaasErp failed companyId={} orderId={} msg={}",
					companyId,
					orderId,
					e.getMessage());
		}
	}

	private static boolean hasAftersalesBn(Object raw) {
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

	private static String text(Object o) {
		return o == null ? "" : o.toString().trim();
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
