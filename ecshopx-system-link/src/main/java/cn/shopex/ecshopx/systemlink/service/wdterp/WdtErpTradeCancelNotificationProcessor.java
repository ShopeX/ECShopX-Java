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

package cn.shopex.ecshopx.systemlink.service.wdterp;

import cn.shopex.ecshopx.common.port.order.WdtErpSettingReadPort;
import cn.shopex.ecshopx.common.port.systemlink.WdtErpTradeCancelOrderAddAssemblePort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.systemlink.wdterp.WdtErpOpenApiClient;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WdtErpTradeCancelNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(WdtErpTradeCancelNotificationProcessor.class);

	private final WdtErpSettingReadPort wdtErpSettingReadPort;
	private final DistributorListQueryService distributorListQueryService;
	private final WdtErpOpenApiClient wdtErpOpenApiClient;
	private final WdtErpTradeCancelOrderAddAssemblePort wdtErpTradeCancelOrderAddAssemblePort;
	private final String orderAddMethod;

	public WdtErpTradeCancelNotificationProcessor(
			WdtErpSettingReadPort wdtErpSettingReadPort,
			DistributorListQueryService distributorListQueryService,
			WdtErpOpenApiClient wdtErpOpenApiClient,
			WdtErpTradeCancelOrderAddAssemblePort wdtErpTradeCancelOrderAddAssemblePort,
			@Value("${ecshopx.wdterp.methods.order-add:sales.Trade.orderAdd}") String orderAddMethod) {
		this.wdtErpSettingReadPort = wdtErpSettingReadPort;
		this.distributorListQueryService = distributorListQueryService;
		this.wdtErpOpenApiClient = wdtErpOpenApiClient;
		this.wdtErpTradeCancelOrderAddAssemblePort = wdtErpTradeCancelOrderAddAssemblePort;
		this.orderAddMethod = orderAddMethod;
	}

	public void handle(Map<String, Object> erpCancelPayload) {
		if (erpCancelPayload == null || erpCancelPayload.isEmpty()) {
			return;
		}
		Long companyId = longVal(erpCancelPayload.get("company_id"));
		if (companyId == null || companyId <= 0) {
			return;
		}
		Map<String, Object> setting = wdtErpSettingReadPort.readWdtErpSetting(companyId);
		if (!isWdtOpen(setting)) {
			log.debug("wdterp trade cancel skipped: ERP not enabled companyId={}", companyId);
			return;
		}
		long distributorId = longOrZero(erpCancelPayload.get("distributor_id"));
		String shopNo = resolveWdtShopNo(companyId, distributorId, setting);
		if (distributorId > 0 && !StringUtils.hasText(shopNo)) {
			log.debug(
					"wdterp trade cancel skipped: distributor without wdt_shop_no companyId={} distributorId={}",
					companyId,
					distributorId);
			return;
		}
		if (!StringUtils.hasText(shopNo)) {
			log.debug("wdterp trade cancel skipped: empty shop_no companyId={}", companyId);
			return;
		}
		List<List<Object>> bodies =
				wdtErpTradeCancelOrderAddAssemblePort.assembleOrderAddBodies(companyId, erpCancelPayload, shopNo);
		if (bodies.isEmpty()) {
			log.debug("wdterp trade cancel skipped: no order_add payloads companyId={}", companyId);
			return;
		}
		String sid = setting.get("sid") != null ? setting.get("sid").toString() : "";
		String appKey = setting.get("app_key") != null ? setting.get("app_key").toString() : "";
		String appSecret = setting.get("app_secret") != null ? setting.get("app_secret").toString() : "";
		for (List<Object> bodyArgs : bodies) {
			try {
				Object result = wdtErpOpenApiClient.call(companyId, orderAddMethod, bodyArgs, sid, appKey, appSecret);
				if (isUnconfiguredClient(result)) {
					log.debug("wdterp trade cancel skipped: client not configured companyId={}", companyId);
					return;
				}
				if (isHardFailure(result)) {
					log.warn("wdterp trade cancel non-success companyId={} detail={}", companyId, summarizeFailure(result));
				}
			} catch (RuntimeException e) {
				log.debug("wdterp trade cancel request failed companyId={} msg={}", companyId, e.getMessage());
			}
		}
	}

	private static boolean isWdtOpen(Map<String, Object> setting) {
		Object open = setting.get("is_open");
		if (open instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(open));
	}

	private String resolveWdtShopNo(long companyId, long distributorId, Map<String, Object> setting) {
		Object shopNoObj = setting.get("shop_no");
		String shopNo = shopNoObj != null ? shopNoObj.toString().trim() : "";
		if (distributorId > 0) {
			List<Distributor> dist =
					distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
			if (dist == null || dist.isEmpty()) {
				return "";
			}
			String wdt = dist.get(0).getWdtShopNo();
			return wdt != null ? wdt.trim() : "";
		}
		return shopNo;
	}

	private static boolean isUnconfiguredClient(Object result) {
		if (!(result instanceof Map<?, ?> m)) {
			return false;
		}
		Object fail = m.get("fail_msg");
		if (fail == null) {
			return false;
		}
		String msg = fail.toString();
		return msg.contains("wdterp client not configured") || msg.contains("missing base-url");
	}

	private static boolean isHardFailure(Object result) {
		if (result == null) {
			return false;
		}
		if (!(result instanceof Map<?, ?> m)) {
			return false;
		}
		Object fail = m.get("fail_msg");
		return fail != null && StringUtils.hasText(fail.toString());
	}

	private static String summarizeFailure(Object result) {
		if (result instanceof Map<?, ?> m) {
			Object fail = m.get("fail_msg");
			if (fail != null && StringUtils.hasText(fail.toString())) {
				return fail.toString();
			}
		}
		return String.valueOf(result);
	}

	private static Long longVal(Object o) {
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

	private static long longOrZero(Object o) {
		Long v = longVal(o);
		return v == null ? 0L : v;
	}
}
