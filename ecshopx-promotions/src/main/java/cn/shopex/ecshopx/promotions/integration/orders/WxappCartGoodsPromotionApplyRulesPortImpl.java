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
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.port.WxappCartGoodsPromotionApplyRulesPort;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappCartGoodsPromotionApplyRulesPortImpl implements WxappCartGoodsPromotionApplyRulesPort {

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MemberAccountService memberAccountService;
	private final ShopMenuService shopMenuService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public WxappCartGoodsPromotionApplyRulesPortImpl(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MemberAccountService memberAccountService,
			ShopMenuService shopMenuService,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.memberAccountService = memberAccountService;
		this.shopMenuService = shopMenuService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	@Override
	public void assertUserCanApplyGoodsPromotion(
			long companyId, long userId, long marketingId, long cartItemId, long cartShopId) {
		Locale locale = LocaleContextHolder.getLocale();
		int now = (int) (System.currentTimeMillis() / 1000L);
		MarketingActivity activity =
				marketingActivityMapper.selectOne(
						new LambdaQueryWrapper<MarketingActivity>()
								.eq(MarketingActivity::getCompanyId, companyId)
								.eq(MarketingActivity::getMarketingId, marketingId));
		if (activity == null) {
			throw new ResourceException(
					msg("promotions.marketing_activity.not_found", "活动不存在", locale));
		}
		if (!"agree".equals(activity.getCheckStatus())) {
			throw new ResourceException(
					msg("promotions.marketing.activity_expired", "活动已失效", locale));
		}
		Integer releaseTime = activity.getReleaseTime();
		if (releaseTime != null && releaseTime != 0 && releaseTime > now) {
			throw new ResourceException(
					msg("promotions.marketing.activity_expired", "活动已失效", locale));
		}
		Integer endTime = activity.getEndTime();
		if (endTime == null || now >= nz(endTime)) {
			throw new ResourceException(msg("promotions.seckill.activity_ended", "活动已结束", locale));
		}
		if (now < nz(activity.getStartTime())) {
			throw new ResourceException(
					msg("promotions.seckill.activity_not_started", "活动未开始", locale));
		}
		int useBound = activity.getUseBound() == null ? 0 : activity.getUseBound();
		if (useBound > 0) {
			List<Long> hit =
					marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(
							companyId, cartItemId, List.of(marketingId), now);
			if (hit == null || hit.isEmpty() || !hit.contains(marketingId)) {
				throw new ResourceException(
						msg(
								"promotions.marketing.cart_apply.item_not_applicable",
								"商品不适用该营销活动",
								locale));
			}
		}
		Integer useShop = activity.getUseShop();
		if (useShop != null && useShop > 0) {
			List<String> shopIds = parseShopIds(activity.getShopIds());
			if (cartShopId > 0L
					&& !shopIds.isEmpty()
					&& !shopIds.contains("all")
					&& !shopIds.contains(String.valueOf(cartShopId))) {
				throw new ResourceException(
						msg(
								"promotions.marketing.cart_apply.shop_not_applicable",
								"当前店铺不适用该营销活动",
								locale));
			}
		}
		String productModel = shopMenuService.resolveProductModelKeyForCompany(companyId);
		long sourceId = activity.getSourceId() == null ? 0L : activity.getSourceId();
		if ("platform".equals(productModel) && cartShopId >= 0L && cartShopId != sourceId) {
			throw new ResourceException(
					msg(
							"promotions.marketing.cart_apply.platform_mismatch",
							"平台活动与店铺不匹配",
							locale));
		}
		List<?> grades = decodeJsonList(activity.getValidGrade());
		if (grades != null && !grades.isEmpty()) {
			Long userGrade = resolveUserGrade(userId, companyId);
			if (userGrade == null || !gradeListContains(grades, userGrade)) {
				throw new ResourceException(
						msg(
								"promotions.marketing.cart_apply.member_grade_not_match",
								"您的会员等级不能参加该活动",
								locale));
			}
		}
		int limit = activity.getJoinLimit() == null ? 0 : activity.getJoinLimit();
		if (limit > 0) {
			String key = "MarketingUserJoinNum:" + companyId + ":" + marketingId;
			Object hv = stringRedisTemplate.opsForHash().get(key, "user_" + userId);
			int used = 0;
			if (hv != null && StringUtils.hasText(hv.toString())) {
				try {
					used = Integer.parseInt(hv.toString().trim());
				} catch (NumberFormatException ignored) {
				}
			}
			if (used >= limit) {
				throw new ResourceException(
						msg(
								"promotions.marketing.cart_apply.join_limit_reached",
								"已超过活动参与次数",
								locale));
			}
		}
	}

	private String msg(String code, String defaultMessage, Locale locale) {
		return messageSource.getMessage(code, null, defaultMessage, locale);
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

	private static List<String> parseShopIds(String raw) {
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return List.of("all");
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	private static boolean gradeListContains(List<?> validGrade, long userGrade) {
		for (Object o : validGrade) {
			if (o instanceof Number n && n.longValue() == userGrade) {
				return true;
			}
			if (o != null) {
				String s = o.toString().trim();
				if (s.equals(String.valueOf(userGrade))) {
					return true;
				}
				try {
					if (Long.parseLong(s) == userGrade) {
						return true;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return false;
	}

	private Long resolveUserGrade(long userId, long companyId) {
		if (userId <= 0L) {
			return null;
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object g = info.get("grade_id");
		if (g instanceof Number n) {
			return n.longValue();
		}
		if (g != null && StringUtils.hasText(g.toString())) {
			try {
				return Long.parseLong(g.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	private static int nz(Integer v) {
		return v != null ? v : 0;
	}
}
