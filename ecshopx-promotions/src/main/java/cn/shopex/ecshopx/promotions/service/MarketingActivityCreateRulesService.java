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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityCreateRulesService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final ObjectMapper JSON = new ObjectMapper();

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;

	public MarketingActivityCreateRulesService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
	}

	public void validateAddPromotionData(Map<String, Object> params) {
		Object ub = params.get("use_bound");
		if (!(ub instanceof Integer i) || i < 0 || i > 4) {
			throw new BadRequestException("use_bound 非法");
		}
		validateRequiredRules(params);
		if ("plus_price_buy".equals(String.valueOf(params.get("marketing_type")))) {
			validatePlusPriceBuySkin(params);
		}
		parseTimes(params);
		normalizeItemIds(params);
		validateMarketingType(params);
		validateConditionType(params);
		if (params.get("use_bound").equals(1)) {
			List<Long> itemIds = readLongList(params.get("item_ids"));
			if (itemIds.isEmpty()) {
				throw new ResourceException("活动商品必填");
			}
			long companyId = readLong(params.get("company_id"));
			if (marketingActivityCatalogAccess.anyGiftItem(companyId, itemIds)) {
				throw new ResourceException("存在赠品，请检查后再次提交");
			}
		}
		if (toInt(params.get("use_shop")) == 1) {
			List<Long> shopIds = readLongList(params.get("shop_ids"));
			if (shopIds.isEmpty()) {
				throw new ResourceException("活动店铺必填");
			}
		}
		int startSec = toInt(params.get("start_time"));
		int endSec = toInt(params.get("end_time"));
		if (endSec <= startSec) {
			throw new ResourceException("活动结束时间不能小于开始时间！");
		}
		long marketingIdParam = params.get("marketing_id") instanceof Number n ? n.longValue() : 0L;
		if (marketingIdParam > 0) {
			long companyId = readLong(params.get("company_id"));
			MarketingActivity existing =
					marketingActivityMapper.selectOne(
							new LambdaQueryWrapper<MarketingActivity>()
									.eq(MarketingActivity::getCompanyId, companyId)
									.eq(MarketingActivity::getMarketingId, marketingIdParam));
			if (existing == null) {
				throw new ResourceException("编辑的活动不存在");
			}
		}
		String mt = String.valueOf(params.get("marketing_type"));
		if ("full_court_gift".equals(mt)) {
			checkFullCourtGiftWindow(params);
		}
		switch (mt) {
			case "full_discount" -> checkFullDiscount(params);
			case "full_minus" -> checkFullMinus(params);
			case "full_gift", "single_gift" -> checkFullGift(params);
			case "plus_price_buy" -> checkPlusPriceBuy(params);
			case "self_select" -> checkSelfSelect(params);
			case "member_preference" -> {
				long companyId = readLong(params.get("company_id"));
				List<Long> itemIds = readLongList(params.get("item_ids"));
				checkItemPreference(companyId, itemIds, mt, startSec, endSec, marketingIdParam);
				checkMemberPreference(params);
				marketingActivityCrossPromotionGuardService.checkGroupActivityForMemberPreference(params);
				marketingActivityCrossPromotionGuardService.checkBargainActivityForMemberPreference(params);
			}
			default -> {
				/* full_court_gift and others already handled or no-op */
			}
		}
	}

	private void validateRequiredRules(Map<String, Object> params) {
		String[][] rules = {
			{"marketing_name", "活动名称必填"},
			{"start_time", "活动开始时间必填"},
			{"end_time", "活动结束时间必填"},
			{"company_id", "企业id必填"},
			{"condition_value", "活动规则必填"},
			{"condition_type", "活动规则条件类型必填"},
			{"marketing_type", "活动类型有误"},
			{"marketing_desc", "活动详细描述必填"},
		};
		for (String[] r : rules) {
			if (!StringUtils.hasText(stringify(params.get(r[0])))) {
				throw new BadRequestException(r[1]);
			}
		}
	}

	private void validatePlusPriceBuySkin(Map<String, Object> params) {
		if (!StringUtils.hasText(stringify(params.get("navbar_color")))) {
			throw new BadRequestException("请选择导航栏颜色");
		}
		if (!StringUtils.hasText(stringify(params.get("activity_background")))) {
			throw new BadRequestException("请上传活动背景图");
		}
	}

	private void validateMarketingType(Map<String, Object> params) {
		String mt = String.valueOf(params.get("marketing_type"));
		List<String> allowed = List.of(
				"full_discount",
				"full_minus",
				"full_gift",
				"plus_price_buy",
				"single_gift",
				"self_select",
				"full_court_gift",
				"member_preference");
		if (!allowed.contains(mt)) {
			throw new ResourceException("活动类型有误");
		}
	}

	private void validateConditionType(Map<String, Object> params) {
		String ct = String.valueOf(params.get("condition_type"));
		if (!"totalfee".equals(ct) && !"quantity".equals(ct)) {
			throw new ResourceException("活动规则条件类型有误");
		}
	}

	private void parseTimes(Map<String, Object> params) {
		Long st = DateExpressionParser.parseToEpochSecond(params.get("start_time"), SHANGHAI);
		Long et = DateExpressionParser.parseToEpochSecond(params.get("end_time"), SHANGHAI);
		if (st == null) {
			throw new BadRequestException("活动开始时间格式错误");
		}
		if (et == null) {
			throw new BadRequestException("活动结束时间格式错误");
		}
		params.put("start_time", st.intValue());
		params.put("end_time", et.intValue());
	}

	private void normalizeItemIds(Map<String, Object> params) {
		if (!params.containsKey("item_ids") || params.get("item_ids") == null) {
			params.put("item_ids", List.of());
			return;
		}
		if (params.get("item_ids") instanceof List<?>) {
			return;
		}
		params.put("item_ids", readLongList(params.get("item_ids")));
	}

	private void checkFullCourtGiftWindow(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		int begin = toInt(params.get("start_time"));
		int end = toInt(params.get("end_time"));
		long exclude = params.get("marketing_id") instanceof Number n ? n.longValue() : 0L;
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivity::getCompanyId, companyId)
				.eq(MarketingActivity::getMarketingType, "full_court_gift")
				.ge(MarketingActivity::getEndTime, Math.max(begin, now))
				.le(MarketingActivity::getStartTime, end);
		if (exclude > 0) {
			w.ne(MarketingActivity::getMarketingId, exclude);
		}
		Long c = marketingActivityMapper.selectCount(w);
		if (c != null && c > 0) {
			throw new ResourceException("在相同时段内，只能有一个全场赠活动。");
		}
	}

	private void checkFullDiscount(Map<String, Object> params) {
		List<Map<String, Object>> ruleArray = readRuleArray(params.get("condition_value"));
		String ct = String.valueOf(params.get("condition_type"));
		int ruleLength = ruleArray.size();
		if ("totalfee".equals(ct)) {
			for (int i = 0; i < ruleLength; i++) {
				Map<String, Object> row = ruleArray.get(i);
				row.put("full", String.format("%.2f", toDouble(row.get("full"))));
				row.put("discount", toDouble(row.get("discount")));
				if (toDouble(row.get("full")) < 1) {
					throw new ResourceException("金额条件必须大于1");
				}
				if (i < ruleLength - 1 && toDouble(row.get("full")) >= toDouble(ruleArray.get(i + 1).get("full"))) {
					throw new ResourceException("满X元Y折，X元必须依次递增！");
				}
				double disc = toDouble(row.get("discount"));
				if (disc >= 100 || disc < 1) {
					throw new ResourceException("折扣必须在区间1%-100%！");
				}
				if (i < ruleLength - 1 && disc >= toDouble(ruleArray.get(i + 1).get("discount"))) {
					throw new ResourceException("给予折扣必须依次递增！");
				}
			}
		} else if ("quantity".equals(ct)) {
			for (int i = 0; i < ruleLength; i++) {
				Map<String, Object> row = ruleArray.get(i);
				row.put("full", (double) toInt(row.get("full")));
				row.put("discount", toDouble(row.get("discount")));
				if (toInt(row.get("full")) < 1) {
					throw new ResourceException("件数条件必须大于1");
				}
				if (i < ruleLength - 1 && toInt(row.get("full")) >= toInt(ruleArray.get(i + 1).get("full"))) {
					throw new ResourceException("购X件Y折，X件必须依次递增！");
				}
				double disc = toDouble(row.get("discount"));
				if (disc >= 100 || disc < 1) {
					throw new ResourceException("折扣必须在区间1%-100%！");
				}
				if (i < ruleLength - 1 && disc >= toDouble(ruleArray.get(i + 1).get("discount"))) {
					throw new ResourceException("给予折扣必须依次递增！");
				}
			}
		}
		params.put("condition_value", ruleArray);
	}

	private void checkFullMinus(Map<String, Object> params) {
		List<Map<String, Object>> ruleArray = readRuleArray(params.get("condition_value"));
		String ct = String.valueOf(params.get("condition_type"));
		int ruleLength = ruleArray.size();
		if ("totalfee".equals(ct)) {
			for (int i = 0; i < ruleLength; i++) {
				Map<String, Object> row = ruleArray.get(i);
				row.put("full", String.format("%.2f", toDouble(row.get("full"))));
				row.put("minus", String.format("%.2f", toDouble(row.get("minus"))));
				if (toDouble(row.get("full")) < 1) {
					throw new ResourceException("金额条件必须大于1");
				}
				if (toDouble(row.get("full")) <= toDouble(row.get("minus"))) {
					throw new ResourceException("满X元减Y元，X必须大于Y！");
				}
				if (i < ruleLength - 1 && toDouble(row.get("full")) >= toDouble(ruleArray.get(i + 1).get("full"))) {
					throw new ResourceException("满X元减Y元，X元必须依次递增！");
				}
				if (i < ruleLength - 1 && toDouble(row.get("minus")) >= toDouble(ruleArray.get(i + 1).get("minus"))) {
					throw new ResourceException("满X元减Y元，Y元必须依次递增！");
				}
			}
		} else if ("quantity".equals(ct)) {
			for (int i = 0; i < ruleLength; i++) {
				Map<String, Object> row = ruleArray.get(i);
				row.put("full", (double) toInt(row.get("full")));
				row.put("minus", String.format("%.2f", toDouble(row.get("minus"))));
				if (toInt(row.get("full")) < 1) {
					throw new ResourceException("件数条件必须大于1");
				}
				if (i < ruleLength - 1 && toInt(row.get("full")) >= toInt(ruleArray.get(i + 1).get("full"))) {
					throw new ResourceException("购X件减Y元，X件必须依次递增！");
				}
				if (i < ruleLength - 1 && toDouble(row.get("minus")) >= toDouble(ruleArray.get(i + 1).get("minus"))) {
					throw new ResourceException("购x件减y元，y元必须依次递增！");
				}
			}
		}
		params.put("condition_value", ruleArray);
	}

	@SuppressWarnings("unchecked")
	private void checkFullGift(Map<String, Object> params) {
		Object giftsRaw = params.get("gifts");
		List<Map<String, Object>> gifts;
		if (giftsRaw instanceof List<?> l) {
			gifts = (List<Map<String, Object>>) (List<?>) l;
		} else {
			gifts = List.of();
		}
		if (gifts.isEmpty()) {
			throw new BadRequestException("赠品不能为空");
		}
		String ct = String.valueOf(params.get("condition_type"));
		Map<Object, Map<String, Object>> ruleArray = new LinkedHashMap<>();
		for (Map<String, Object> gift : gifts) {
			double filterFull = toDouble(gift.get("filter_full"));
			if ("totalfee".equals(ct) && filterFull < 0.01) {
				throw new ResourceException("金额条件必须大于0.01");
			}
			if ("quantity".equals(ct) && filterFull < 1) {
				throw new ResourceException("件数条件必须大于1");
			}
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("full", filterFull);
			ruleArray.put(filterFull, cell);
		}
		List<Map<String, Object>> sorted = new ArrayList<>(ruleArray.values());
		sorted.sort((a, b) -> Double.compare(toDouble(b.get("full")), toDouble(a.get("full"))));
		params.put("condition_value", sorted);
	}

	@SuppressWarnings("unchecked")
	private void checkPlusPriceBuy(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		Object giftsRaw = params.get("gifts");
		List<Map<String, Object>> gifts;
		if (giftsRaw instanceof List<?> l) {
			gifts = (List<Map<String, Object>>) (List<?>) l;
		} else if (giftsRaw instanceof String s && StringUtils.hasText(s)) {
			gifts = parseGiftsJsonString(s);
			params.put("gifts", gifts);
			validatePlusPriceBuyGiftPrices(companyId, gifts);
		} else {
			gifts = List.of();
		}
		if (gifts.isEmpty()) {
			throw new BadRequestException("赠品不能为空");
		}
		List<Map<String, Object>> ruleArray = readRuleArray(params.get("condition_value"));
		String ct = String.valueOf(params.get("condition_type"));
		int ruleLength = ruleArray.size();
		if ("totalfee".equals(ct)) {
			for (int i = 0; i < ruleLength; i++) {
				Map<String, Object> row = ruleArray.get(i);
				row.put("full", String.format("%.2f", toDouble(row.get("full"))));
				row.put("price", String.format("%.2f", toDouble(row.get("price"))));
				if (toDouble(row.get("full")) < 1) {
					throw new ResourceException("金额条件必须大于1");
				}
			}
		} else if ("quantity".equals(ct)) {
			for (int i = 0; i < ruleLength; i++) {
				Map<String, Object> row = ruleArray.get(i);
				row.put("full", (double) toInt(row.get("full")));
				row.put("price", String.format("%.2f", toDouble(row.get("price"))));
				if (toInt(row.get("full")) < 1) {
					throw new ResourceException("件数条件必须大于1");
				}
			}
		}
		params.put("condition_value", ruleArray);
	}

	private static List<Map<String, Object>> parseGiftsJsonString(String json) {
		try {
			return JSON.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			throw new BadRequestException("gifts 格式错误");
		}
	}

	private void validatePlusPriceBuyGiftPrices(long companyId, List<Map<String, Object>> gifts) {
		List<Long> giftItemIds = new ArrayList<>();
		for (Map<String, Object> g : gifts) {
			Object iid = g.get("item_id");
			if (iid instanceof Number n) {
				giftItemIds.add(n.longValue());
			}
		}
		Map<Long, Long> priceByItem = marketingActivityCatalogAccess.loadItemPriceByItemId(companyId, giftItemIds);
		for (Map<String, Object> g : gifts) {
			long itemId = readLong(g.get("item_id"));
			long priceCent = priceByItem.getOrDefault(itemId, 0L);
			double giftYuan = toDouble(g.get("price"));
			if (priceCent < giftYuan * 100) {
				throw new ResourceException("赠品金额不能低于加价购金额");
			}
			g.put("price", String.format("%.2f", giftYuan));
		}
	}

	@SuppressWarnings("unchecked")
	private void checkSelfSelect(Map<String, Object> params) {
		Object cv = params.get("condition_value");
		if (!(cv instanceof List<?> outer) || outer.isEmpty()) {
			throw new ResourceException("活动规则必填");
		}
		Object first = outer.get(0);
		if (!(first instanceof Map<?, ?> m)) {
			throw new ResourceException("活动规则必填");
		}
		Map<String, Object> ruleArray = (Map<String, Object>) m;
		if (toDouble(ruleArray.get("full")) < 1) {
			throw new ResourceException("金额必须大于0");
		}
		if (toDouble(ruleArray.get("num")) < 1) {
			throw new ResourceException("件数必须大于0");
		}
	}

	private void checkMemberPreference(Map<String, Object> params) {
		List<?> vg = readRawList(params.get("valid_grade"));
		if (vg.isEmpty()) {
			throw new ResourceException("请至少选择一个会员等级");
		}
	}

	private void checkItemPreference(
			long companyId,
			List<Long> itemIds,
			String marketingType,
			int beginTime,
			int endTime,
			long excludeMarketingId) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		QueryWrapper<MarketingActivityItems> qw = new QueryWrapper<>();
		qw.in("item_id", itemIds)
				.eq("company_id", companyId)
				.ge("end_time", now)
				.apply(
						"EXISTS (SELECT 1 FROM promotions_marketing_activity ma WHERE ma.marketing_id = promotions_marketing_activity_items.marketing_id AND ma.company_id = promotions_marketing_activity_items.company_id AND ma.marketing_type = {0})",
						marketingType)
				.and(w -> w.and(q -> q.le("start_time", beginTime).gt("end_time", beginTime))
						.or(q -> q.lt("start_time", endTime).ge("end_time", endTime))
						.or(q -> q.ge("start_time", beginTime).le("end_time", endTime)));
		if (excludeMarketingId > 0) {
			qw.ne("marketing_id", excludeMarketingId);
		}
		List<MarketingActivityItems> hits = marketingActivityItemsMapper.selectList(qw);
		if (!hits.isEmpty()) {
			MarketingActivityItems h = hits.get(0);
			String errormsg = truncate(h.getItemName(), 20) + "(" + nullToEmpty(h.getItemSpecDesc()) + ")";
			throw new ResourceException(
					"在相同时段内，同一个商品只能参加一个活动。marketing_id:"
							+ h.getMarketingId()
							+ "。"
							+ errormsg);
		}
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static List<Map<String, Object>> readRuleArray(Object raw) {
		if (!(raw instanceof List<?> l) || l.isEmpty()) {
			throw new ResourceException("活动规则必填");
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : l) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> cell = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					cell.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(cell);
			}
		}
		if (out.isEmpty()) {
			throw new ResourceException("活动规则必填");
		}
		return out;
	}

	private static List<?> readRawList(Object v) {
		if (v instanceof List<?> l) {
			return l;
		}
		return List.of();
	}

	private static List<Long> readLongList(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof List<?> l) {
			List<Long> out = new ArrayList<>();
			for (Object o : l) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o != null && StringUtils.hasText(o.toString())) {
					try {
						out.add(Long.parseLong(o.toString().trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return out;
		}
		return List.of();
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(String.valueOf(v).trim());
	}

	private static double toDouble(Object v) {
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		return Double.parseDouble(String.valueOf(v).trim());
	}

	private static String stringify(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof String s) {
			return s.trim();
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty() ? "" : "x";
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty() ? "" : "x";
		}
		return String.valueOf(v).trim();
	}
}
