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

package cn.shopex.ecshopx.promotions.integration.orders;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.GroupOrderListItemFieldsPort;
import cn.shopex.ecshopx.orders.port.WxappGroupOrderDetailPort;
import cn.shopex.ecshopx.orders.port.WxappGroupOrderDetailServiceOrderPort;
import cn.shopex.ecshopx.promotions.domain.PaymentOverEndTimeRow;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityAdminRowAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGroupOrderDetailPortImpl implements WxappGroupOrderDetailPort {

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final GroupOrderListItemFieldsPort groupOrderListItemFieldsPort;
	private final WxappGroupOrderDetailServiceOrderPort wxappGroupOrderDetailServiceOrderPort;
	private final PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;
	private final ObjectMapper objectMapper;

	public WxappGroupOrderDetailPortImpl(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			GroupOrderListItemFieldsPort groupOrderListItemFieldsPort,
			WxappGroupOrderDetailServiceOrderPort wxappGroupOrderDetailServiceOrderPort,
			PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler,
			ObjectMapper objectMapper) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.groupOrderListItemFieldsPort = groupOrderListItemFieldsPort;
		this.wxappGroupOrderDetailServiceOrderPort = wxappGroupOrderDetailServiceOrderPort;
		this.promotionGroupsActivityAdminRowAssembler = promotionGroupsActivityAdminRowAssembler;
		this.objectMapper = objectMapper;
	}

	@Override
	public Object getGroupOrderDetail(String teamId, long requestCompanyId) {
		PromotionGroupsTeam team =
				promotionGroupsTeamMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeam>()
								.eq(PromotionGroupsTeam::getTeamId, teamId)
								.last("LIMIT 1"));
		if (team == null) {
			return Collections.emptyList();
		}

		List<PromotionGroupsTeamMember> memberEntities =
				promotionGroupsTeamMemberMapper.selectList(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getTeamId, teamId)
								.eq(PromotionGroupsTeamMember::getDisabled, false)
								.orderByDesc(PromotionGroupsTeamMember::getJoinTime)
								.last("LIMIT 99"));
		if (memberEntities == null) {
			return Collections.emptyList();
		}

		String orderId = null;
		for (PromotionGroupsTeamMember m : memberEntities) {
			if (m != null && StringUtils.hasText(m.getOrderId())) {
				orderId = m.getOrderId();
				break;
			}
		}

		Long actId = team.getActId();
		if (actId == null || actId <= 0L) {
			throw new ResourceException("拼团活动不存在");
		}
		PromotionGroupsActivity activity =
				promotionGroupsActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getGroupsActivityId, actId)
								.eq(PromotionGroupsActivity::getDisabled, false)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("拼团活动不存在");
		}

		Long actCompanyId = activity.getCompanyId();
		if (actCompanyId == null || actCompanyId <= 0L) {
			throw new ResourceException("拼团活动不存在");
		}
		if (actCompanyId.longValue() != requestCompanyId) {
			throw new ResourceException("拼团活动不存在");
		}
		long skuCompanyId = actCompanyId.longValue();

		Long goodsId = activity.getGoodsId();
		if (goodsId == null || goodsId <= 0L) {
			throw new ResourceException("拼团活动商品无效");
		}
		Map<Long, Map<String, Object>> itemDisplayById =
				groupOrderListItemFieldsPort.loadDisplayByItemIds(skuCompanyId, List.of(goodsId));
		Map<String, Object> itemDisplay = itemDisplayById.get(goodsId);

		Map<String, Object> orderRow =
				wxappGroupOrderDetailServiceOrderPort.getServiceOrder(skuCompanyId, orderId);

		long now = Instant.now().getEpochSecond();
		long totalMembers =
				promotionGroupsTeamMemberMapper.selectCount(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getTeamId, teamId)
								.eq(PromotionGroupsTeamMember::getDisabled, false));

		List<Map<String, Object>> memberMaps = new ArrayList<>(memberEntities.size());
		for (PromotionGroupsTeamMember m : memberEntities) {
			memberMaps.add(memberRow(m));
		}

		LinkedHashMap<String, Object> memberListWrap = new LinkedHashMap<>();
		memberListWrap.put("total_count", totalMembers);
		memberListWrap.put("list", memberMaps);

		LinkedHashMap<String, Object> activityInfo =
				buildActivityInfo(activity, team, itemDisplay, orderRow, now);
		LinkedHashMap<String, Object> teamInfo = buildTeamInfo(team, now, teamId);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("team_info", teamInfo);
		out.put("activity_info", activityInfo);
		out.put("member_list", memberListWrap);
		return out;
	}

	private LinkedHashMap<String, Object> buildTeamInfo(
			PromotionGroupsTeam team, long now, String teamId) {
		LinkedHashMap<String, Object> teamRow = new LinkedHashMap<>();
		teamRow.put("id", stringifyScalar(team.getId()));
		teamRow.put("team_id", team.getTeamId());
		teamRow.put("company_id", stringifyScalar(team.getCompanyId()));
		teamRow.put("act_id", stringifyScalar(team.getActId()));
		teamRow.put("head_mid", stringifyScalar(team.getHeadMid()));
		teamRow.put("begin_time", stringifyScalar(team.getBeginTime()));
		teamRow.put("end_time", stringifyScalar(team.getEndTime()));
		teamRow.put("join_person_num", stringifyScalar(team.getJoinPersonNum()));
		teamRow.put("team_status", stringifyScalar(team.getTeamStatus()));
		teamRow.put("group_goods_type", team.getGroupGoodsType());
		teamRow.put("disabled", team.getDisabled());
		teamRow.put("created", team.getCreated());
		teamRow.put("updated", team.getUpdated());
		int timeStatus = teamTimeStatus(team, now);
		teamRow.put("status", timeStatus);
		teamRow.put("progress", teamProgress(timeStatus, teamId));
		return teamRow;
	}

	private int teamTimeStatus(PromotionGroupsTeam team, long now) {
		Long begin = team.getBeginTime();
		Long end = team.getEndTime();
		if (begin != null && begin > 0L && now < begin) {
			return 1;
		}
		if (end != null && end > 0L && now > end) {
			return 3;
		}
		return 2;
	}

	private int teamProgress(int timeStatus, String teamId) {
		if (timeStatus == 1) {
			return 1;
		}
		if (timeStatus == 2) {
			return 2;
		}
		List<PaymentOverEndTimeRow> latePayments =
				promotionGroupsTeamMemberMapper.selectPaymentOverEndTimeList(List.of(teamId));
		if (latePayments != null && !latePayments.isEmpty()) {
			return 4;
		}
		return 3;
	}

	private LinkedHashMap<String, Object> buildActivityInfo(
			PromotionGroupsActivity activity,
			PromotionGroupsTeam team,
			Map<String, Object> itemDisplay,
			Map<String, Object> orderRow,
			long now) {
		LinkedHashMap<String, Object> m =
				new LinkedHashMap<>(
						promotionGroupsActivityAdminRowAssembler.toRow(activity, (int) now));
		stringifyActivityScalars(m);

		Long gid = activity.getGoodsId();
		m.put("itemId", stringifyScalar(gid));

		String itemNameFromItem = itemDisplay == null ? null : stringOrNull(itemDisplay.get("itemName"));
		if (StringUtils.hasText(itemNameFromItem)) {
			m.put("itemName", itemNameFromItem);
		} else {
			m.put("itemName", activity.getActName());
		}

		Object priceFromItem = itemDisplay == null ? null : itemDisplay.get("price");
		if (priceFromItem != null) {
			m.put("price", priceFromItem);
		} else {
			m.put("price", activity.getActPrice());
		}

		Object picsFromItem = itemDisplay == null ? null : parsePicsJson(itemDisplay.get("pics"));
		if (picsFromItem != null) {
			m.put("pics", picsFromItem);
		} else {
			m.put("pics", parsePicsJson(activity.getPics()));
		}

		m.put("over_time", teamOverTimeSeconds(team, now));
		if (orderRow != null) {
			m.put("shop_id", orderRow.get("shop_id"));
		} else {
			m.put("shop_id", null);
		}
		m.put("status", activityTimeStatus(activity, now));
		return m;
	}

	private static int teamOverTimeSeconds(PromotionGroupsTeam team, long now) {
		Long end = team.getEndTime();
		if (end != null && end > now) {
			return (int) (end - now);
		}
		return 0;
	}

	private static void stringifyActivityScalars(Map<String, Object> m) {
		putStringified(m, "groups_activity_id");
		putStringified(m, "company_id");
		putStringified(m, "goods_id");
		putStringified(m, "act_price");
		putStringified(m, "begin_time");
		putStringified(m, "end_time");
		putStringified(m, "limit_buy_num");
		putStringified(m, "store");
	}

	private static void putStringified(Map<String, Object> m, String key) {
		if (m.containsKey(key)) {
			m.put(key, stringifyScalar(m.get(key)));
		}
	}

	private static int activityTimeStatus(PromotionGroupsActivity activity, long now) {
		Long begin = activity.getBeginTime();
		Long end = activity.getEndTime();
		if (begin != null && begin > 0L && now < begin) {
			return 1;
		}
		if (end != null && end > 0L && now > end) {
			return 3;
		}
		return 2;
	}

	private Map<String, Object> memberRow(PromotionGroupsTeamMember m) {
		LinkedHashMap<String, Object> mm = new LinkedHashMap<>();
		mm.put("id", stringifyScalar(m.getId()));
		mm.put("team_id", m.getTeamId());
		mm.put("company_id", stringifyScalar(m.getCompanyId()));
		mm.put("act_id", stringifyScalar(m.getActId()));
		mm.put("member_id", stringifyScalar(m.getMemberId()));
		mm.put("join_time", stringifyScalar(m.getJoinTime()));
		mm.put("order_id", m.getOrderId());
		mm.put("group_goods_type", m.getGroupGoodsType());
		mm.put("member_info", parseJsonObjectOrNull(m.getMemberInfo()));
		mm.put("disabled", m.getDisabled());
		return mm;
	}

	private Object parsePicsJson(Object picsRaw) {
		if (picsRaw == null) {
			return null;
		}
		if (picsRaw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		String s = picsRaw.toString();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (Exception ignored) {
			return null;
		}
	}

	private Object parseJsonObjectOrNull(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static String stringifyScalar(Object v) {
		if (v == null) {
			return null;
		}
		return String.valueOf(v);
	}

	private static String stringOrNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? null : s;
	}
}
