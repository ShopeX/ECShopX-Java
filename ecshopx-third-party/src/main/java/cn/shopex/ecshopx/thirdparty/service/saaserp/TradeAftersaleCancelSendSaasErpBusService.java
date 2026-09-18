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
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeAftersaleCancelSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeAftersaleCancelSendSaasErpBusService.class);

	private static final List<String> ENTITY_SNAPSHOT_KEYS =
			List.of(
					"aftersales_status",
					"progress",
					"refuse_reason",
					"aftersales_type",
					"aftersales_bn",
					"company_id",
					"order_id");

	private final SaasErpStoreTradeAftersaleCancelPort saasErpStoreTradeAftersaleCancelPort;
	private final SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort;
	private final SaasErpTradeAftersaleOrderStructPort saasErpTradeAftersaleOrderStructPort;

	public TradeAftersaleCancelSendSaasErpBusService(
			SaasErpStoreTradeAftersaleCancelPort saasErpStoreTradeAftersaleCancelPort,
			SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort,
			SaasErpTradeAftersaleOrderStructPort saasErpTradeAftersaleOrderStructPort) {
		this.saasErpStoreTradeAftersaleCancelPort = saasErpStoreTradeAftersaleCancelPort;
		this.saasErpCompanySaasBindingReadPort = saasErpCompanySaasBindingReadPort;
		this.saasErpTradeAftersaleOrderStructPort = saasErpTradeAftersaleOrderStructPort;
	}

	public void handleTradeAftersalesCancelEntities(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		log.debug("TradeAftersaleCancelSendSaasErp_entities: {}", entities);
		Long companyId = longObj(entities.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			log.debug("TradeAftersaleCancelSendSaasErp_skip reason=missing_company_id");
			return;
		}
		long orderId = longVal(entities.get("order_id"));
		if (orderId <= 0L) {
			log.debug("TradeAftersaleCancelSendSaasErp_skip reason=missing_order_id companyId={}", companyId);
			return;
		}
		Object aftersalesBnRaw = entities.get("aftersales_bn");
		if (!hasAftersalesBn(aftersalesBnRaw)) {
			log.debug(
					"TradeAftersaleCancelSendSaasErp_skip reason=missing_aftersales_bn companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		if (!saasErpCompanySaasBindingReadPort.isSaasErpOutboundEnabled(companyId)) {
			log.debug(
					"TradeAftersaleCancelSendSaasErp_skip reason=saas_erp_outbound_disabled companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		try {
			Map<String, Object> outboundBody = new LinkedHashMap<>(entities);
			Map<String, Object> sourced =
					saasErpTradeAftersaleOrderStructPort.loadOrderAftersalePayloadOrNull(
							companyId, orderId, aftersalesBnRaw);
			if (sourced != null && !sourced.isEmpty()) {
				outboundBody.putAll(sourced);
				for (String k : ENTITY_SNAPSHOT_KEYS) {
					if (entities.containsKey(k)) {
						outboundBody.put(k, entities.get(k));
					}
				}
			}
			outboundBody.put("aftersales_action", "cancel");
			saasErpStoreTradeAftersaleCancelPort.callStoreTradeAftersaleCancel(outboundBody);
		} catch (RuntimeException e) {
			log.debug(
					"store.trade.aftersale.cancel failed companyId={} orderId={} msg={}",
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
