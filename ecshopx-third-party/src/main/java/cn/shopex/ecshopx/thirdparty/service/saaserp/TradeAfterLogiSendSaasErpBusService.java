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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeAfterLogiSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeAfterLogiSendSaasErpBusService.class);

	private static final String STORE_TRADE_AFTERSALE_LOGISTICS_UPDATE = "store.trade.aftersale.logistics.update";

	private final SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort;
	private final SaasErpAftersaleLogisticsStructPort saasErpAftersaleLogisticsStructPort;
	private final SaasErpStoreTradeAftersaleLogisticsUpdatePort saasErpStoreTradeAftersaleLogisticsUpdatePort;

	public TradeAfterLogiSendSaasErpBusService(
			SaasErpCompanySaasBindingReadPort saasErpCompanySaasBindingReadPort,
			SaasErpAftersaleLogisticsStructPort saasErpAftersaleLogisticsStructPort,
			SaasErpStoreTradeAftersaleLogisticsUpdatePort saasErpStoreTradeAftersaleLogisticsUpdatePort) {
		this.saasErpCompanySaasBindingReadPort = saasErpCompanySaasBindingReadPort;
		this.saasErpAftersaleLogisticsStructPort = saasErpAftersaleLogisticsStructPort;
		this.saasErpStoreTradeAftersaleLogisticsUpdatePort = saasErpStoreTradeAftersaleLogisticsUpdatePort;
	}

	public void handleTradeAftersalesLogisticsUpdate(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		log.debug("TradeAfterLogiSendSaasErp_entities: {}", entities);
		Long companyId = longObj(entities.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			log.debug("TradeAfterLogiSendSaasErp_skip reason=missing_company_id");
			return;
		}
		if (!saasErpCompanySaasBindingReadPort.isSaasErpOutboundEnabled(companyId)) {
			log.debug("TradeAfterLogiSendSaasErp_skip reason=saas_erp_outbound_disabled companyId={}", companyId);
			return;
		}
		try {
			Map<String, Object> afterLogistics =
					saasErpAftersaleLogisticsStructPort.buildAfterLogisticsOrNull(companyId, entities);
			if (afterLogistics == null || afterLogistics.isEmpty()) {
				log.debug("TradeAfterLogiSendSaasErp_skip reason=after_logistics_unavailable companyId={}", companyId);
				return;
			}
			saasErpStoreTradeAftersaleLogisticsUpdatePort.callStoreTradeAftersaleLogisticsUpdate(
					companyId, afterLogistics);
			log.debug(
					"{} TradeAfterLogiSendSaasErp result=ok companyId={}",
					STORE_TRADE_AFTERSALE_LOGISTICS_UPDATE,
					companyId);
		} catch (RuntimeException e) {
			log.debug(
					"TradeAfterLogiSendSaasErp method={} companyId={} msg={}",
					STORE_TRADE_AFTERSALE_LOGISTICS_UPDATE,
					companyId,
					e.getMessage());
		}
	}

	private static Long longObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
