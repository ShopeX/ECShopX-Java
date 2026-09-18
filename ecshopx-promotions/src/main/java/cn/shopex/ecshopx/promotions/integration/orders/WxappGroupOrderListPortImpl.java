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

import cn.shopex.ecshopx.common.goods.port.GroupOrderListItemFieldsPort;
import cn.shopex.ecshopx.orders.port.WxappGroupOrderListPort;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappGroupOrderListQuery;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGroupOrderListPortImpl implements WxappGroupOrderListPort {

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final GroupOrderListItemFieldsPort groupOrderListItemFieldsPort;
	private final ObjectMapper objectMapper;

	public WxappGroupOrderListPortImpl(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			GroupOrderListItemFieldsPort groupOrderListItemFieldsPort,
			ObjectMapper objectMapper) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.groupOrderListItemFieldsPort = groupOrderListItemFieldsPort;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> getGroupOrderList(WxappGroupOrderListQuery query) {
		if (query.userId() == 0L) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", List.of());
			empty.put("total_count", Collections.emptyList());
			return empty;
		}

		int page = Math.max(1, parseIntOrDefault(query.pageRaw(), 1));
		int pageSizeRaw = parseIntOrDefault(query.pageSizeRaw(), 50);
		int limit = normalizePageLimit(pageSizeRaw);
		Long teamStatus = resolveTeamStatusFilter(query.teamStatusKeyPresent(), query.teamStatusRaw());
		final String groupGoodsType;
		if (query.groupGoodsTypeKeyPresent()) {
			String r = query.groupGoodsTypeRaw();
			groupGoodsType = r != null ? r : "";
		} else {
			groupGoodsType = "services";
		}

		long total =
				promotionGroupsTeamMapper.countWxappGroupOrderListByMember(
						query.companyId(), query.userId(), teamStatus, groupGoodsType);
		int offset = (page - 1) * limit;
		List<LinkedHashMap<String, Object>> pageRows =
				promotionGroupsTeamMapper.selectWxappGroupOrderListByMemberPage(
						query.companyId(), query.userId(), teamStatus, groupGoodsType, offset, limit);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (pageRows.isEmpty()) {
			out.put("list", List.of());
			return out;
		}

		Set<String> teamIds = new LinkedHashSet<>();
		for (LinkedHashMap<String, Object> row : pageRows) {
			Object tid = row.get("team_id");
			if (tid != null) {
				teamIds.add(String.valueOf(tid));
			}
		}

		int memberGlobalLimit = limit * 10;
		List<PromotionGroupsTeamMember> memberRows =
				teamIds.isEmpty()
						? List.of()
						: promotionGroupsTeamMemberMapper.selectList(
								new LambdaQueryWrapper<PromotionGroupsTeamMember>()
										.in(PromotionGroupsTeamMember::getTeamId, teamIds)
										.eq(PromotionGroupsTeamMember::getDisabled, false)
										.orderByDesc(PromotionGroupsTeamMember::getJoinTime)
										.last("LIMIT " + memberGlobalLimit));

		Map<String, List<Map<String, Object>>> memberListByTeamId = new LinkedHashMap<>();
		for (PromotionGroupsTeamMember m : memberRows) {
			String tid = m.getTeamId();
			if (tid == null) {
				continue;
			}
			memberListByTeamId.computeIfAbsent(tid, k -> new ArrayList<>()).add(memberRow(m));
		}

		Set<Long> actIds = new LinkedHashSet<>();
		for (LinkedHashMap<String, Object> row : pageRows) {
			Long aid = longObject(row.get("act_id"));
			if (aid != null && aid > 0L) {
				actIds.add(aid);
			}
		}

		Map<Long, PromotionGroupsActivity> activityById = new LinkedHashMap<>();
		if (!actIds.isEmpty()) {
			List<PromotionGroupsActivity> acts =
					promotionGroupsActivityMapper.selectList(
							new LambdaQueryWrapper<PromotionGroupsActivity>()
									.eq(PromotionGroupsActivity::getCompanyId, query.companyId())
									.in(PromotionGroupsActivity::getGroupsActivityId, actIds)
									.eq(PromotionGroupsActivity::getDisabled, false));
			for (PromotionGroupsActivity a : acts) {
				if (a.getGroupsActivityId() != null) {
					activityById.put(a.getGroupsActivityId(), a);
				}
			}
		}

		Set<Long> goodsIds = new LinkedHashSet<>();
		for (PromotionGroupsActivity a : activityById.values()) {
			Long gid = a.getGoodsId();
			if (gid != null && gid > 0L) {
				goodsIds.add(gid);
			}
		}
		Map<Long, Map<String, Object>> itemDisplayById =
				groupOrderListItemFieldsPort.loadDisplayByItemIds(query.companyId(), goodsIds);

		List<Map<String, Object>> list = new ArrayList<>(pageRows.size());
		for (LinkedHashMap<String, Object> row : pageRows) {
			list.add(assembleRow(row, memberListByTeamId, activityById, itemDisplayById));
		}
		out.put("list", list);
		return out;
	}

	private Map<String, Object> assembleRow(
			LinkedHashMap<String, Object> row,
			Map<String, List<Map<String, Object>>> memberListByTeamId,
			Map<Long, PromotionGroupsActivity> activityById,
			Map<Long, Map<String, Object>> itemDisplayById) {
		LinkedHashMap<String, Object> item = new LinkedHashMap<>(row);
		Object memberInfoRaw = row.get("member_info");
		item.put("member_info", parseJsonObjectOrNull(memberInfoRaw == null ? null : String.valueOf(memberInfoRaw)));

		String teamIdKey = row.get("team_id") == null ? null : String.valueOf(row.get("team_id"));
		List<Map<String, Object>> members =
				teamIdKey == null ? List.of() : memberListByTeamId.getOrDefault(teamIdKey, List.of());
		item.put("member_list", members);

		Long actId = longObject(row.get("act_id"));
		PromotionGroupsActivity act = actId == null ? null : activityById.get(actId);
		if (act != null) {
			item.put("person_num", act.getPersonNum());
			Long gid = act.getGoodsId();
			item.put("itemId", gid);
			Map<String, Object> disp = gid == null ? null : itemDisplayById.get(gid);
			String itemNameFromItem = disp == null ? null : stringOrNull(disp.get("itemName"));
			if (StringUtils.hasText(itemNameFromItem)) {
				item.put("itemName", itemNameFromItem);
			} else {
				item.put("itemName", act.getActName());
			}
			Object priceFromItem = disp == null ? null : disp.get("price");
			if (priceFromItem != null) {
				item.put("price", priceFromItem);
			} else {
				item.put("price", act.getActPrice());
			}
			String picsFromItem = disp == null ? null : stringOrNull(disp.get("pics"));
			if (StringUtils.hasText(picsFromItem)) {
				item.put("pics", picsFromItem);
			} else {
				item.put("pics", act.getPics());
			}
		}
		return item;
	}

	private static String stringOrNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? null : s;
	}

	private Map<String, Object> memberRow(PromotionGroupsTeamMember m) {
		LinkedHashMap<String, Object> mm = new LinkedHashMap<>();
		mm.put("member_id", m.getMemberId());
		mm.put("order_id", m.getOrderId());
		mm.put("join_time", m.getJoinTime());
		mm.put("member_info", parseJsonObjectOrNull(m.getMemberInfo()));
		mm.put("team_id", m.getTeamId());
		mm.put("company_id", m.getCompanyId());
		mm.put("act_id", m.getActId());
		mm.put("group_goods_type", m.getGroupGoodsType());
		mm.put("disabled", m.getDisabled());
		return mm;
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

	private static int parseIntOrDefault(String raw, int defaultVal) {
		if (raw == null || raw.isBlank()) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int normalizePageLimit(int pageSize) {
		if (pageSize > 100) {
			return 100;
		}
		if (pageSize <= 0) {
			return 10;
		}
		return pageSize;
	}

	private static Long resolveTeamStatusFilter(boolean keyPresent, String raw) {
		if (!keyPresent) {
			return null;
		}
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if ("1".equals(t) || "2".equals(t) || "3".equals(t)) {
			return Long.parseLong(t);
		}
		try {
			int n = Integer.parseInt(t);
			if (n == 1 || n == 2 || n == 3) {
				return (long) n;
			}
		} catch (NumberFormatException ignored) {
			// ignore invalid values
		}
		return null;
	}

	private static Long longObject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
