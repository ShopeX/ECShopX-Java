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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsDetailFacadeService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseActivityItemDetailService {

	private final GoodsItemsDetailFacadeService goodsItemsDetailFacadeService;
	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final ActivityItemsMapper activityItemsMapper;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public EmployeePurchaseActivityItemDetailService(
			GoodsItemsDetailFacadeService goodsItemsDetailFacadeService,
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			ActivityItemsMapper activityItemsMapper,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.goodsItemsDetailFacadeService = goodsItemsDetailFacadeService;
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.activityItemsMapper = activityItemsMapper;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> buildActivityItemDetail(
			HttpServletRequest request,
			Map<String, Object> claims,
			long itemId,
			long enterpriseId,
			long activityId) {
		long companyId = parseCompanyIdPositive(claims);

		LinkedHashMap<String, Object> jwtForGoods = new LinkedHashMap<>(claims);

		String authorizerAppId = null;
		Object rawWoa = claims.get("woa_appid");
		if (rawWoa != null) {
			String t = rawWoa.toString().trim();
			authorizerAppId = t.isEmpty() ? null : t;
		}

		Map<String, Object> result =
				goodsItemsDetailFacadeService.getDetail(request, jwtForGoods, itemId, authorizerAppId);

		if (result == null
				|| result.isEmpty()
				|| result.get("item_id") == null
				|| Integer.valueOf(422).equals(result.get("status_code"))) {
			throw new ResourceException("商品不存在");
		}

		result.put("activity_type", "employee_purchase");

		Activities activity =
				employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(
						companyId, activityId, enterpriseId);
		result.put("activity_info", buildActivityInfoMap(activity));

		long goodsId = requirePositiveLong(result.get("goods_id"), "商品不存在");

		List<ActivityItems> activityItemRows =
				activityItemsMapper.selectList(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getGoodsId, goodsId));
		Map<Long, ActivityItems> byItemId =
				activityItemRows.stream()
						.collect(
								Collectors.toMap(
										ActivityItems::getItemId,
										Function.identity(),
										(a, b) -> a,
										LinkedHashMap::new));

		long currentItemId = toLongStrict(result.get("item_id"));
		if (currentItemId <= 0L) {
			throw new ResourceException("商品不存在");
		}
		ActivityItems mainRow = byItemId.get(currentItemId);
		overlayActivityItemSaleFields(
				result, mainRow, Boolean.TRUE.equals(activity.getIfShareStore()));

		final Object nospec = result.get("nospec");
		final boolean issetNospec = result.containsKey("nospec") && nospec != null;
		boolean enterMultiSpecBranch =
				(issetNospec
								&& (Objects.equals(nospec, Boolean.FALSE)
										|| Objects.equals(nospec, "false")))
						|| Objects.equals(nospec, Integer.valueOf(0))
						|| Objects.equals(nospec, "0");

		if (enterMultiSpecBranch) {
			Object specObj = result.get("spec_items");
			if (specObj instanceof List<?> rawList) {
				boolean ifShareStore = Boolean.TRUE.equals(activity.getIfShareStore());
				for (Object el : rawList) {
					if (!(el instanceof Map<?, ?>)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> specItem = (Map<String, Object>) el;
					long specItemId = toLongFlexible(specItem.get("item_id"), -1L);
					if (specItemId <= 0L) {
						continue;
					}
					ActivityItems row = byItemId.get(specItemId);
					overlayActivityItemSaleFields(specItem, row, ifShareStore);
				}
				int sumStore = 0;
				for (Object el : rawList) {
					if (!(el instanceof Map<?, ?>)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> specItem = (Map<String, Object>) el;
					sumStore += intValueStore(specItem.get("store"));
				}
				result.put("store", sumStore);
			}
		}

		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		result.put("cur", currencyToMap(cur));

		Object its = result.get("item_total_sales");
		if (its != null) {
			result.put("sales", its);
		} else {
			result.put("sales", result.get("sales"));
		}

		result.put("rate_status", readRateStatus(companyId));
		result.put("sales_setting", readItemSalesSetting(companyId));
		result.put("store_setting", readItemStoreSetting(companyId));

		result.put(
				"distributor_id",
				activity.getDistributorId() != null ? activity.getDistributorId().longValue() : 0L);

		return result;
	}

	private Map<String, Object> buildActivityInfoMap(Activities activity) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", activity.getId());
		m.put("company_id", activity.getCompanyId());
		m.put(
				"distributor_id",
				activity.getDistributorId() != null ? activity.getDistributorId() : 0);
		m.put("operator_id", activity.getOperatorId() != null ? activity.getOperatorId() : 0);
		m.put("name", activity.getName());
		m.put("title", activity.getTitle());
		m.put("pages_template_id", activity.getPagesTemplateId());
		m.put("pic", activity.getPic());
		m.put("share_pic", activity.getSharePic());
		m.put(
				"enterprise_id",
				employeePurchaseActivityDataService.listEnterpriseIdsFromCsv(activity.getEnterpriseId()));
		m.put("display_time", activity.getDisplayTime());
		m.put("employee_begin_time", activity.getEmployeeBeginTime());
		m.put("employee_end_time", activity.getEmployeeEndTime());
		m.put("employee_limitfee", activity.getEmployeeLimitfee());
		m.put(
				"if_relative_join",
				PurchaseModeSupport.isPrepaidPoint(activity)
						? 0
						: (Boolean.TRUE.equals(activity.getIfRelativeJoin()) ? 1 : 0));
		m.put("invite_limit", activity.getInviteLimit() != null ? activity.getInviteLimit() : 0);
		m.put("relative_begin_time", activity.getRelativeBeginTime());
		m.put("relative_end_time", activity.getRelativeEndTime());
		m.put("if_share_limitfee", Boolean.TRUE.equals(activity.getIfShareLimitfee()) ? 1 : 0);
		m.put("relative_limitfee", activity.getRelativeLimitfee() != null ? activity.getRelativeLimitfee() : 0);
		m.put("minimum_amount", activity.getMinimumAmount() != null ? activity.getMinimumAmount() : 0);
		m.put("purchase_mode", activity.getPurchaseMode());
		m.put("purchase_mode_desc", PurchaseModeSupport.desc(activity.getPurchaseMode()));
		m.put(
				"close_modify_hours_after_activity",
				activity.getCloseModifyHoursAfterActivity() != null
						? activity.getCloseModifyHoursAfterActivity()
						: 0);
		m.put("status", activity.getStatus());
		m.put("if_share_store", Boolean.TRUE.equals(activity.getIfShareStore()) ? 1 : 0);
		m.put("price_display_config", activity.getPriceDisplayConfig());
		m.put(
				"is_discount_description_enabled",
				Boolean.TRUE.equals(activity.getIsDiscountDescriptionEnabled()) ? 1 : 0);
		m.put("discount_description", activity.getDiscountDescription());
		m.put("created", activity.getCreated());
		m.put("updated", activity.getUpdated());
		return m;
	}

	/**
	 * Align PHP FrontApi {@code getActivityItemDetail} {@code $isOnShelf}: {@code shelf_status}
	 * is not returned; off-shelf SKUs get {@code store=0}, {@code approve_status=instock}, zero limits.
	 */
	static boolean isOnShelf(ActivityItems row) {
		if (row == null) {
			return false;
		}
		Integer shelfStatus = row.getShelfStatus();
		return shelfStatus == null || shelfStatus.intValue() == 1;
	}

	static void overlayActivityItemSaleFields(
			Map<String, Object> target, ActivityItems row, boolean ifShareStore) {
		if (isOnShelf(row)) {
			target.put("activity_price", row.getActivityPrice());
			if (!ifShareStore) {
				target.put("store", row.getActivityStore() != null ? row.getActivityStore() : 0);
			}
			putActivityItemLimitFields(target, row);
			return;
		}
		target.put("store", 0);
		target.put("approve_status", "instock");
		putActivityItemLimitFields(target, null);
	}

	static void putActivityItemLimitFields(Map<String, Object> target, ActivityItems row) {
		target.put("limit_num", row == null || row.getLimitNum() == null ? 0 : row.getLimitNum());
		target.put("limit_fee", row == null || row.getLimitFee() == null ? 0 : row.getLimitFee());
	}

	private static LinkedHashMap<String, Object> currencyToMap(CurrencyExchangeRate c) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("currency", c.getCurrency());
		m.put("title", c.getTitle());
		m.put("symbol", c.getSymbol());
		m.put("rate", c.getRate());
		m.put("is_default", Boolean.TRUE.equals(c.getIsDefault()));
		if (c.getUsePlatform() != null) {
			m.put("use_platform", c.getUsePlatform());
		}
		return m;
	}

	private boolean readRateStatus(long companyId) {
		String key = "TradeRateSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return false;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("rate_status"), false);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean readItemSalesSetting(long companyId) {
		String key = "ItemSalesSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return true;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("item_sales_status"), true);
		} catch (Exception e) {
			return true;
		}
	}

	private boolean readItemStoreSetting(long companyId) {
		String key = "ItemStoreSetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return true;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return jsonNodeToBooleanStrict(n.get("item_store_status"), true);
		} catch (Exception e) {
			return true;
		}
	}

	private static boolean jsonNodeToBooleanStrict(JsonNode node, boolean defaultVal) {
		if (node == null || node.isNull()) {
			return defaultVal;
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isInt()) {
			return node.asInt() != 0;
		}
		if (node.isTextual()) {
			String s = node.asText().trim();
			return "1".equals(s) || "true".equalsIgnoreCase(s);
		}
		return defaultVal;
	}

	private static long parseCompanyIdPositive(Map<String, Object> claims) {
		Object raw = claims.get("company_id");
		if (raw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long v = Long.parseLong(raw.toString().trim());
			if (v <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static long requirePositiveLong(Object o, String message) {
		if (o == null) {
			throw new ResourceException(message);
		}
		long v;
		if (o instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(o.toString().trim());
			} catch (NumberFormatException e) {
				throw new ResourceException(message);
			}
		}
		if (v <= 0L) {
			throw new ResourceException(message);
		}
		return v;
	}

	private static long toLongStrict(Object o) {
		if (o == null) {
			return -1L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	private static long toLongFlexible(Object o, long defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int intValueStore(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
