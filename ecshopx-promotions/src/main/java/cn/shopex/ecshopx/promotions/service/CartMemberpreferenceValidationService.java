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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CartMemberpreferenceValidationService {

	private static final String MEMBER_PREFERENCE = "member_preference";
	private static final String MSG_MEMBERS_ONLY = "仅限特定会员购买";
	private static final String MSG_ACTIVITY_PRODUCT_ERROR = "活动商品出错";

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MemberAccountService memberAccountService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final ObjectMapper objectMapper;

	public CartMemberpreferenceValidationService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MemberAccountService memberAccountService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.memberAccountService = memberAccountService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.objectMapper = objectMapper;
	}

	public void assertEligibleForCartAdd(long companyId, long userId, long itemId) {
		String reason = ineligibleReasonForCartList(companyId, userId, itemId);
		if (reason != null) {
			throw new ResourceException(reason);
		}
	}

	public boolean isEligibleForCartList(long companyId, long userId, long itemId) {
		return ineligibleReasonForCartList(companyId, userId, itemId) == null;
	}

	private String ineligibleReasonForCartList(long companyId, long userId, long itemId) {
		if (companyId <= 0L || itemId <= 0L) {
			return null;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> aw = new LambdaQueryWrapper<>();
		aw.eq(MarketingActivity::getCompanyId, companyId)
				.eq(MarketingActivity::getMarketingType, MEMBER_PREFERENCE)
				.le(MarketingActivity::getStartTime, now)
				.ge(MarketingActivity::getEndTime, now)
				.orderByDesc(MarketingActivity::getStartTime);
		List<MarketingActivity> activityList = marketingActivityMapper.selectList(aw);
		if (activityList == null || activityList.isEmpty()) {
			return null;
		}
		List<Long> marketingIdOrder =
				activityList.stream().map(MarketingActivity::getMarketingId).filter(Objects::nonNull).toList();
		List<Long> hitMarketingIds =
				marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(
						companyId, itemId, marketingIdOrder, now);
		if (hitMarketingIds == null || hitMarketingIds.isEmpty()) {
			return null;
		}
		Map<Long, Map<String, Object>> relItemArr = relItemArrForSkuHits(hitMarketingIds, itemId);
		if (userId <= 0L) {
			return MSG_MEMBERS_ONLY;
		}
		String userGradeToken = resolveUserGradeToken(userId, companyId);
		boolean anyMatched = false;
		for (MarketingActivity value : activityList) {
			Long mid = value.getMarketingId();
			if (mid == null || !relItemArr.containsKey(mid)) {
				continue;
			}
			anyMatched = true;
			int useBound = value.getUseBound() != null ? value.getUseBound() : 0;
			if (useBound == 1) {
				Map<String, Object> itemsForMid = relItemArr.get(mid);
				if (itemsForMid == null || !itemsForMid.containsKey(String.valueOf(itemId))) {
					return MSG_ACTIVITY_PRODUCT_ERROR;
				}
			}
			List<?> validGrade = decodeJsonList(value.getValidGrade());
			if (!validGrade.isEmpty()
					&& StringUtils.hasText(userGradeToken)
					&& !gradeListContainsToken(validGrade, userGradeToken)) {
				return MSG_MEMBERS_ONLY;
			}
		}
		return anyMatched ? null : MSG_MEMBERS_ONLY;
	}

	/**
	 * Keys each hit marketing id to the cart SKU, matching PHP
	 * {@code getValidActivityRelItems} + {@code rel_item_ids} expansion for the requested item.
	 */
	private static Map<Long, Map<String, Object>> relItemArrForSkuHits(List<Long> hitMarketingIds, long itemId) {
		if (hitMarketingIds == null || hitMarketingIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		String itemKey = String.valueOf(itemId);
		for (Long mid : hitMarketingIds) {
			if (mid == null) {
				continue;
			}
			out.computeIfAbsent(mid, k -> new LinkedHashMap<>()).put(itemKey, Map.of());
		}
		return out;
	}

	private List<?> decodeJsonList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception e) {
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
}
