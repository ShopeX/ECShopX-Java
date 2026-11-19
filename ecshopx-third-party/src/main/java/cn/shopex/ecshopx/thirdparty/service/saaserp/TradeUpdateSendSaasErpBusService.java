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
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpOrderStructPort;
import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertErpBindReadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeUpdateSendSaasErpBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeUpdateSendSaasErpBusService.class);

	private final TradeUpdateGroupMemberOrdersPort tradeUpdateGroupMemberOrdersPort;
	private final SaasErpStoreTradeAddPort saasErpStoreTradeAddPort;
	private final SaasErpOrderStructPort saasErpOrderStructPort;
	private final SaasCertErpBindReadService saasCertErpBindReadService;

	public TradeUpdateSendSaasErpBusService(
			TradeUpdateGroupMemberOrdersPort tradeUpdateGroupMemberOrdersPort,
			SaasErpStoreTradeAddPort saasErpStoreTradeAddPort,
			SaasErpOrderStructPort saasErpOrderStructPort,
			SaasCertErpBindReadService saasCertErpBindReadService) {
		this.tradeUpdateGroupMemberOrdersPort = tradeUpdateGroupMemberOrdersPort;
		this.saasErpStoreTradeAddPort = saasErpStoreTradeAddPort;
		this.saasErpOrderStructPort = saasErpOrderStructPort;
		this.saasCertErpBindReadService = saasCertErpBindReadService;
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
		if (!saasCertErpBindReadService.isErpBound(companyId)) {
			log.debug(
					"saaserp TradeUpdateSendSaasErp companyId:{},orderId:{},msg:未开启SaasErp",
					companyId,
					orderIdStr);
			return;
		}
		String sourceType = toSaasErpSourceType(str(entities.get("order_class")));
		switch (sourceType) {
			case "normal_shopguide", "normal_shopadmin", "normal_seckill", "normal_normal", "normal" ->
					pushOrderStruct(companyId, orderIdStr, sourceType);
			case "normal_groups" -> handleGroupBranch(entities, companyId, orderIdStr);
			default -> {
				/* unhandled order_class */
			}
		}
	}

	/**
	 * PHP prefixes a non-{@code normal} {@code order_class} with {@code normal_}. Values that are already
	 * {@code normal} or {@code normal_*} are kept so publishers that stored the prefixed slug still match.
	 */
	public static String toSaasErpSourceType(String orderClass) {
		if (!StringUtils.hasText(orderClass)) {
			return "";
		}
		String raw = orderClass.trim();
		if ("normal".equals(raw) || raw.startsWith("normal_")) {
			return raw;
		}
		return "normal_" + raw;
	}

	private void pushOrderStruct(long companyId, String orderId, String sourceType) {
		Optional<Map<String, Object>> struct =
				saasErpOrderStructPort.getOrderStruct(companyId, orderId, sourceType);
		if (struct.isEmpty()) {
			log.debug(
					"store.trade.add skipped: empty order struct companyId={} orderId={} sourceType={}",
					companyId,
					orderId,
					sourceType);
			return;
		}
		try {
			saasErpStoreTradeAddPort.callStoreTradeAdd(companyId, struct.get());
		} catch (RuntimeException e) {
			log.debug(
					"store.trade.add failed companyId={} orderId={} msg={}",
					companyId,
					orderId,
					e.getMessage());
		}
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
			String rowSource = StringUtils.hasText(groupGoodsType) ? groupGoodsType : "normal_groups";
			pushOrderStruct(rowCompany, rowOrderId, rowSource);
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
