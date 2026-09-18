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
public class TradeAftersalesSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeAftersalesSendSaasErpBusService.class);

	private final SaasErpStoreTradeAftersaleAddPort saasErpStoreTradeAftersaleAddPort;
	private final SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort;
	private final SaasErpTradeAftersaleOrderStructPort saasErpTradeAftersaleOrderStructPort;

	public TradeAftersalesSendSaasErpBusService(
			SaasErpStoreTradeAftersaleAddPort saasErpStoreTradeAftersaleAddPort,
			SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort,
			SaasErpTradeAftersaleOrderStructPort saasErpTradeAftersaleOrderStructPort) {
		this.saasErpStoreTradeAftersaleAddPort = saasErpStoreTradeAftersaleAddPort;
		this.saasErpCompanySaasBindingReadPort = saasErpCompanySaasBindingReadPort;
		this.saasErpTradeAftersaleOrderStructPort = saasErpTradeAftersaleOrderStructPort;
	}

	public void handleTradeAftersalesEntities(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		log.debug("TradeAftersalesSendSaasErp_entities: {}", entities);
		Long companyId = longObj(entities.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			log.debug("TradeAftersalesSendSaasErp_skip reason=missing_company_id");
			return;
		}
		long orderId = longVal(entities.get("order_id"));
		if (orderId <= 0L) {
			log.debug("TradeAftersalesSendSaasErp_skip reason=missing_order_id companyId={}", companyId);
			return;
		}
		Object aftersalesBnRaw = entities.get("aftersales_bn");
		if (!hasAftersalesBn(aftersalesBnRaw)) {
			log.debug(
					"TradeAftersalesSendSaasErp_skip reason=missing_aftersales_bn companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		if (!saasErpCompanySaasBindingReadPort.isSaasErpOutboundEnabled(companyId)) {
			log.debug(
					"TradeAftersalesSendSaasErp_skip reason=saas_erp_outbound_disabled companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		try {
			Map<String, Object> sourced =
					saasErpTradeAftersaleOrderStructPort.loadOrderAftersalePayloadOrNull(
							companyId, orderId, aftersalesBnRaw);
			Map<String, Object> outboundBody = new LinkedHashMap<>();
			if (sourced == null) {
				outboundBody.putAll(entities);
				log.debug(
						"TradeAftersalesSendSaasErp reason=order_aftersale_payload_from_event_entities companyId={} orderId={}",
						companyId,
						orderId);
			} else if (sourced.isEmpty()) {
				log.debug(
						"TradeAftersalesSendSaasErp_skip reason=empty_order_aftersale_struct companyId={} orderId={}",
						companyId,
						orderId);
				return;
			} else {
				outboundBody.putAll(sourced);
			}
			int originalStatus = intFrom(outboundBody.get("status"));
			outboundBody.put("status", 1);
			saasErpStoreTradeAftersaleAddPort.callStoreTradeAftersaleAdd(outboundBody);
			if (originalStatus != 1) {
				Map<String, Object> updatePayload =
						saasErpTradeAftersaleOrderStructPort.buildAftersaleOmeStatusUpdatePayloadOrNull(
								companyId, aftersalesBnRaw);
				if (updatePayload != null && !updatePayload.isEmpty()) {
					try {
						saasErpStoreTradeAftersaleAddPort.callOmeAftersaleStatusUpdate(updatePayload);
					} catch (RuntimeException e) {
						log.debug(
								"ome.aftersale.status_update failed companyId={} orderId={} msg={}",
								companyId,
								orderId,
								e.getMessage());
					}
				} else {
					log.debug(
							"TradeAftersalesSendSaasErp_skip reason=aftersale_ome_status_update_payload_unavailable companyId={} orderId={} originalStatus={}",
							companyId,
							orderId,
							originalStatus);
				}
			}
		} catch (RuntimeException e) {
			log.debug(
					"store.trade.aftersale.add failed companyId={} orderId={} msg={}",
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

	private static int intFrom(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o instanceof Boolean b) {
			return b ? 1 : 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
