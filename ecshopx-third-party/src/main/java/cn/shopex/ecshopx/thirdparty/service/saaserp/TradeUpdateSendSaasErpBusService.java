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

import cn.shopex.ecshopx.common.saaserp.TradeUpdateGroupMemberOrdersPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeUpdateSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeUpdateSendSaasErpBusService.class);

	private final TradeUpdateGroupMemberOrdersPort tradeUpdateGroupMemberOrdersPort;
	private final SaasErpStoreTradeAddPort saasErpStoreTradeAddPort;

	public TradeUpdateSendSaasErpBusService(
			TradeUpdateGroupMemberOrdersPort tradeUpdateGroupMemberOrdersPort,
			SaasErpStoreTradeAddPort saasErpStoreTradeAddPort) {
		this.tradeUpdateGroupMemberOrdersPort = tradeUpdateGroupMemberOrdersPort;
		this.saasErpStoreTradeAddPort = saasErpStoreTradeAddPort;
	}

	public void handleTradeUpdateEntities(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		log.debug("TradeUpdateSendSaasErp_entities: {}", entities);
		Long companyId = longObj(entities.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			return;
		}
		String orderIdStr = stringifyOrderId(entities.get("order_id"));
		if (!StringUtils.hasText(orderIdStr)) {
			return;
		}
		String sourceType = str(entities.get("order_class"));
		switch (sourceType) {
			case "normal_seckill", "normal_normal", "normal" -> handleSingleOrderBranch(entities);
			case "normal_groups", "groups" -> handleGroupBranch(entities, companyId, orderIdStr);
			default -> {
				/* unhandled order_class */
			}
		}
	}

	private void handleSingleOrderBranch(Map<String, Object> entities) {
		saasErpStoreTradeAddPort.callStoreTradeAdd(new LinkedHashMap<>(entities));
	}

	private void handleGroupBranch(Map<String, Object> entities, long companyId, String orderIdStr) {
		Long userId = longObj(entities.get("user_id"));
		if (userId == null) {
			return;
		}
		List<LinkedHashMap<String, Object>> rows =
				tradeUpdateGroupMemberOrdersPort.listPaidTeamOrderRowsForLeader(companyId, orderIdStr, userId);
		if (rows == null || rows.isEmpty()) {
			return;
		}
		for (LinkedHashMap<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			if (!"PAYED".equals(str(row.get("o_order_status")))) {
				continue;
			}
			long rowCompany = longPrimitive(row.get("m_company_id"), companyId);
			String rowOrderId = stringifyOrderId(row.get("m_order_id"));
			String groupGoodsType = str(row.get("m_group_goods_type"));
			if (!StringUtils.hasText(rowOrderId)) {
				continue;
			}
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("company_id", rowCompany);
			body.put("order_id", rowOrderId);
			body.put("order_class", StringUtils.hasText(groupGoodsType) ? groupGoodsType : "normal_groups");
			Object uid = row.get("m_member_id");
			if (uid != null) {
				body.put("user_id", uid);
			}
			try {
				saasErpStoreTradeAddPort.callStoreTradeAdd(body);
			} catch (RuntimeException e) {
				log.debug(
						"store.trade.add failed companyId={} orderId={} msg={}",
						rowCompany,
						rowOrderId,
						e.getMessage());
			}
		}
	}

	private static long longPrimitive(Object o, long defaultVal) {
		Long v = longObj(o);
		return v != null ? v : defaultVal;
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

	private static String stringifyOrderId(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
