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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsDetailMemberpreferenceActivityService {

	private static final String MEMBER_PREFERENCE = "member_preference";

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MemberAccountService memberAccountService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final ObjectMapper objectMapper;

	public WxappGoodsDetailMemberpreferenceActivityService(MarketingActivityMapper marketingActivityMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess, MemberAccountService memberAccountService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService, MemberCardGradeQueryService memberCardGradeQueryService,
			VipGradeListQueryService vipGradeListQueryService, ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.memberAccountService = memberAccountService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listValidMemberpreferenceForGoodsDetail(long companyId, long userId, long goodsId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivity::getCompanyId, companyId)
				.eq(MarketingActivity::getMarketingType, MEMBER_PREFERENCE)
				.le(MarketingActivity::getStartTime, now)
				.ge(MarketingActivity::getEndTime, now)
				.orderByDesc(MarketingActivity::getStartTime);
		List<MarketingActivity> activityList = marketingActivityMapper.selectList(w);
		if (activityList == null || activityList.isEmpty()) {
			return List.of();
		}
		List<Long> marketingIdOrder =
				activityList.stream().map(MarketingActivity::getMarketingId).filter(Objects::nonNull).toList();
		Map<Long, List<Long>> relItemIdsByMarketing = buildRelItemIdsByMarketing(companyId, goodsId, marketingIdOrder, now);
		Map<Long, Map<String, Object>> relItemArr =
				loadRelItemMapsForGoodsDetail(companyId, marketingIdOrder, now, relItemIdsByMarketing);
		String userGradeToken = resolveUserGradeToken(userId, companyId);
		LinkedHashMap<String, Map<String, Object>> memberGradeIndex = buildMemberGradeIndex(companyId);
		List<Map<String, Object>> out = new ArrayList<>();
		for (MarketingActivity value : activityList) {
			int useBound = value.getUseBound() != null ? value.getUseBound() : 0;
			if (goodsId > 0 && useBound > 0) {
				if (!goodsInActivityRel(goodsId, value.getMarketingId(), relItemArr)) {
					continue;
				}
			}
			List<?> validGrade = decodeJsonList(value.getValidGrade());
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("marketing_desc", value.getMarketingDesc());
			row.put("marketing_name", value.getMarketingName());
			if (!validGrade.isEmpty() && StringUtils.hasText(userGradeToken) && !gradeListContainsToken(validGrade, userGradeToken)) {
				row.put("user_grade_valid", false);
			} else {
				row.put("user_grade_valid", true);
			}
			List<String> memberGradeNames = buildMemberGradeNameList(validGrade, memberGradeIndex);
			if (!memberGradeNames.isEmpty()) {
				row.put("member_grade", memberGradeNames);
			}
			row.put("status", resolveActivityStatus(now, value.getStartTime(), value.getEndTime()));
			out.add(row);
		}
		return out;
	}

	private static boolean goodsInActivityRel(long goodsId, Long marketingId, Map<Long, Map<String, Object>> relItemArr) {
		if (marketingId == null) {
			return false;
		}
		Map<String, Object> bucket = relItemArr.get(marketingId);
		if (bucket == null || bucket.isEmpty()) {
			return false;
		}
		for (Object rowObj : bucket.values()) {
			if (!(rowObj instanceof Map<?, ?> row)) {
				continue;
			}
			Object gid = row.get("goods_id");
			if (gid == null) {
				continue;
			}
			if (goodsId == toLong(gid)) {
				return true;
			}
		}
		return false;
	}

	private Map<Long, List<Long>> buildRelItemIdsByMarketing(long companyId, long goodsId, List<Long> marketingIdOrder, int now) {
		if (goodsId <= 0L || marketingIdOrder == null || marketingIdOrder.isEmpty()) {
			return Map.of();
		}
		List<Long> skuIds = marketingActivityCatalogAccess.listItemIdsByGoodsId(companyId, goodsId);
		if (skuIds == null || skuIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, LinkedHashSet<Long>> acc = new LinkedHashMap<>();
		for (Long skuId : skuIds) {
			if (skuId == null || skuId <= 0L) {
				continue;
			}
			List<Long> mids = marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(companyId, skuId, marketingIdOrder, now);
			if (mids == null) {
				continue;
			}
			for (Long mid : mids) {
				if (mid != null) {
					acc.computeIfAbsent(mid, k -> new LinkedHashSet<>()).add(skuId);
				}
			}
		}
		Map<Long, List<Long>> out = new LinkedHashMap<>();
		for (Map.Entry<Long, LinkedHashSet<Long>> en : acc.entrySet()) {
			out.put(en.getKey(), new ArrayList<>(en.getValue()));
		}
		return out;
	}

	private Map<Long, Map<String, Object>> loadRelItemMapsForGoodsDetail(long companyId, List<Long> marketingIds, int now,
			Map<Long, List<Long>> relItemIdsByMarketing) {
		if (marketingIds == null || marketingIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<MarketingActivityItems> iw = new LambdaQueryWrapper<>();
		iw.eq(MarketingActivityItems::getCompanyId, companyId)
				.in(MarketingActivityItems::getMarketingId, marketingIds)
				.le(MarketingActivityItems::getStartTime, now)
				.ge(MarketingActivityItems::getEndTime, now)
				.orderByAsc(MarketingActivityItems::getMarketingId)
				.orderByAsc(MarketingActivityItems::getItemId)
				.orderByDesc(MarketingActivityItems::getStartTime);
		List<MarketingActivityItems> rows = marketingActivityItemsMapper.selectList(iw);
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		if (rows == null) {
			return out;
		}
		for (MarketingActivityItems r : rows) {
			Long mid = r.getMarketingId();
			if (mid == null || r.getItemId() == null) {
				continue;
			}
			Map<String, Object> bucket = out.computeIfAbsent(mid, k -> new LinkedHashMap<>());
			Map<String, Object> itemRow = relItemRowToMap(r);
			List<Long> relIds = relItemIdsByMarketing.get(mid);
			if (relIds == null || relIds.isEmpty()) {
				bucket.put(String.valueOf(r.getItemId()), itemRow);
				continue;
			}
			for (Long relItemId : relIds) {
				if (relItemId != null) {
					bucket.put(String.valueOf(relItemId), itemRow);
				}
			}
		}
		return out;
	}

	private static Map<String, Object> relItemRowToMap(MarketingActivityItems r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", r.getItemId());
		m.put("goods_id", r.getGoodsId());
		m.put("marketing_id", r.getMarketingId());
		return m;
	}

	private static String resolveActivityStatus(int now, @Nullable Integer startTime, @Nullable Integer endTime) {
		int end = endTime != null ? endTime : 0;
		int start = startTime != null ? startTime : 0;
		if (now >= end) {
			return "end";
		}
		if (now >= start && now < end) {
			return "ongoing";
		}
		return "waiting";
	}

	private LinkedHashMap<String, Map<String, Object>> buildMemberGradeIndex(long companyId) {
		LinkedHashMap<String, Map<String, Object>> memberGrade = new LinkedHashMap<>();
		for (Map<String, Object> cardRow : memberCardGradeQueryService.getGradeListByCompanyId(companyId, false)) {
			Object gid = cardRow.get("grade_id");
			putGradeIndexKey(memberGrade, gid, cardRow);
		}
		for (Map<String, Object> vipRow : vipGradeListQueryService.listDataVipGrade(companyId, true)) {
			Object lv = vipRow.get("lv_type");
			if (lv != null && StringUtils.hasText(String.valueOf(lv))) {
				memberGrade.put(String.valueOf(lv), vipRow);
			}
		}
		return memberGrade;
	}

	private static void putGradeIndexKey(LinkedHashMap<String, Map<String, Object>> memberGrade, Object gid, Map<String, Object> row) {
		if (gid instanceof Number n) {
			memberGrade.put(String.valueOf(n.longValue()), row);
		} else if (gid != null && StringUtils.hasText(gid.toString())) {
			try {
				memberGrade.put(String.valueOf(Long.parseLong(gid.toString().trim())), row);
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private static List<String> buildMemberGradeNameList(@Nullable List<?> validGrade,
			LinkedHashMap<String, Map<String, Object>> memberGrade) {
		if (validGrade == null || validGrade.isEmpty()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (Object keyObj : validGrade) {
			String keyStr = keyObj == null ? "" : String.valueOf(keyObj);
			Map<String, Object> hit = memberGrade.get(keyStr);
			if (hit != null && hit.get("grade_name") != null) {
				names.add(String.valueOf(hit.get("grade_name")));
			}
		}
		return names;
	}

	private List<?> decodeJsonList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception ignored) {
			return List.of();
		}
	}

	private static boolean gradeListContainsToken(List<?> validGrade, String userGradeToken) {
		String token = userGradeToken.trim();
		for (Object o : validGrade) {
			if (o == null) {
				continue;
			}
			if (o.toString().trim().equals(token)) {
				return true;
			}
		}
		return false;
	}

	private String resolveUserGradeToken(long userId, long companyId) {
		if (userId <= 0) {
			return "";
		}
		Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (Boolean.TRUE.equals(vip.get("valid"))
				&& Boolean.TRUE.equals(vip.get("is_vip"))
				&& vip.get("vip_type") != null
				&& StringUtils.hasText(String.valueOf(vip.get("vip_type")).trim())) {
			return String.valueOf(vip.get("vip_type")).trim();
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object g = info.get("grade_id");
		if (g instanceof Number n && n.longValue() > 0L) {
			return String.valueOf(n.longValue());
		}
		if (g != null && StringUtils.hasText(g.toString())) {
			String s = g.toString().trim();
			try {
				long id = Long.parseLong(s);
				if (id > 0L) {
					return String.valueOf(id);
				}
			} catch (NumberFormatException ignored) {
				if (!"0".equals(s)) {
					return s;
				}
			}
		}
		return "";
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o != null) {
			try {
				return Long.parseLong(o.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return 0L;
	}
}
