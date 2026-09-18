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

package cn.shopex.ecshopx.systemlink.service.ome;

import cn.shopex.ecshopx.goods.service.ome.ShopexErpOpenApiClient;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import cn.shopex.ecshopx.systemlink.service.third.ThirdShopexErpSettingAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmeTradeUpdateNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(OmeTradeUpdateNotificationProcessor.class);

	private static final String OME_ORDER_ADD_METHOD = "ome.order.add";

	private static final int TEAM_ORDER_PAGE_LIMIT = 10_000;

	private final ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final ShopexErpOpenApiClient shopexErpOpenApiClient;
	private final OmeOrderAddOpenApiPayloadBuilder omeOrderAddOpenApiPayloadBuilder;

	public OmeTradeUpdateNotificationProcessor(
			ThirdShopexErpSettingAdminService thirdShopexErpSettingAdminService,
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			ShopexErpOpenApiClient shopexErpOpenApiClient,
			OmeOrderAddOpenApiPayloadBuilder omeOrderAddOpenApiPayloadBuilder) {
		this.thirdShopexErpSettingAdminService = thirdShopexErpSettingAdminService;
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.shopexErpOpenApiClient = shopexErpOpenApiClient;
		this.omeOrderAddOpenApiPayloadBuilder = omeOrderAddOpenApiPayloadBuilder;
	}

	public void handle(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		log.debug("TradeUpdateSendOme_event: {}", payload);
		Long companyId = longObj(payload.get("company_id"));
		if (companyId == null || companyId <= 0L) {
			return;
		}
		String orderIdStr = stringifyOrderId(payload.get("order_id"));
		if (!StringUtils.hasText(orderIdStr)) {
			return;
		}
		Map<String, Object> setting = thirdShopexErpSettingAdminService.getShopexErpSetting(companyId);
		if (setting == null || !isShopexErpOpen(setting)) {
			log.debug("companyId:{},orderId:{},msg:未开启OME", companyId, orderIdStr);
			return;
		}
		String sourceType = str(payload.get("order_class"));
		switch (sourceType) {
			case "normal_seckill", "normal_normal", "normal" -> handleSingleOrderBranch(companyId, orderIdStr, sourceType);
			case "normal_groups", "groups" -> handleGroupBranch(payload, companyId, orderIdStr);
			default -> {
				/* no-op for unhandled order_class */
			}
		}
	}

	private void handleSingleOrderBranch(long companyId, String orderIdStr, String sourceType) {
		Optional<Map<String, Object>> struct =
				omeOrderAddOpenApiPayloadBuilder.tryBuild(companyId, orderIdStr, sourceType);
		if (struct.isEmpty()) {
			log.debug(
					"获取订单信息失败:companyId:{},orderId:{},sourceType:{}",
					companyId,
					orderIdStr,
					sourceType);
			return;
		}
		omeRequest(struct.get(), companyId);
	}

	private void handleGroupBranch(Map<String, Object> payload, long companyId, String orderIdStr) {
		Long userId = longObj(payload.get("user_id"));
		if (userId == null) {
			return;
		}
		PromotionGroupsTeamMember teamRow =
				promotionGroupsTeamMemberMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getCompanyId, companyId)
								.eq(PromotionGroupsTeamMember::getOrderId, orderIdStr)
								.eq(PromotionGroupsTeamMember::getMemberId, userId)
								.last("LIMIT 1"));
		if (teamRow == null || !StringUtils.hasText(teamRow.getTeamId())) {
			log.debug(
					"获取团购团员信息失败:companyId:{},orderId:{},memberId:{}",
					companyId,
					orderIdStr,
					userId);
			return;
		}
		log.debug("TradeUpdateSendOme_teamInfo: {}", teamRow);
		String teamId = teamRow.getTeamId();
		List<LinkedHashMap<String, Object>> rawList =
				promotionGroupsTeamMemberMapper.selectTeamMemberOrderListPage(
						companyId, teamId, 0L, 0L, null, 0, TEAM_ORDER_PAGE_LIMIT);
		log.debug("TradeUpdateSendOme_orderList size={}", rawList == null ? 0 : rawList.size());
		if (rawList == null || rawList.isEmpty()) {
			return;
		}
		boolean anyPayed = false;
		for (LinkedHashMap<String, Object> row : rawList) {
			if (row == null) {
				continue;
			}
			if (!"PAYED".equals(str(row.get("o_order_status")))) {
				continue;
			}
			anyPayed = true;
			long rowCompany = longPrimitive(row.get("m_company_id"), companyId);
			String rowOrderId = stringifyOrderId(row.get("m_order_id"));
			String groupGoodsType = str(row.get("m_group_goods_type"));
			if (!StringUtils.hasText(rowOrderId)) {
				continue;
			}
			Optional<Map<String, Object>> struct =
					omeOrderAddOpenApiPayloadBuilder.tryBuild(rowCompany, rowOrderId, groupGoodsType);
			if (struct.isEmpty()) {
				log.debug(
						"获取团购订单信息失败:companyId:{},orderId:{},sourceType:{}",
						rowCompany,
						rowOrderId,
						groupGoodsType);
				continue;
			}
			omeRequest(struct.get(), companyId);
		}
		if (!anyPayed) {
			/* No OME requests when no paid team orders remain after filtering. */
		}
	}

	private void omeRequest(Map<String, Object> orderStruct, long companyId) {
		try {
			Map<String, Object> result =
					shopexErpOpenApiClient.call(companyId, OME_ORDER_ADD_METHOD, orderStruct);
			log.debug(
					"{}=>orderStruct:{}=>result:{}",
					OME_ORDER_ADD_METHOD,
					orderStruct,
					result);
		} catch (RuntimeException e) {
			log.debug(
					"OME请求失败:{}=>method:{}=>orderStruct:{}",
					e.getMessage(),
					OME_ORDER_ADD_METHOD,
					orderStruct);
		}
	}

	private static boolean isShopexErpOpen(Map<String, Object> setting) {
		Object open = setting.get("is_open");
		if (open instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		return Boolean.parseBoolean(String.valueOf(open));
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
