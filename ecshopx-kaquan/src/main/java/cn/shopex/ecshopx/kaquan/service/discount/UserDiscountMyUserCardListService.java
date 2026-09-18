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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.distribution.service.distributor.DistributorCouponListAppendService;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountMyCardListFilterParams;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountMyUserCardListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(SHANGHAI);

	private final UserDiscountMapper userDiscountMapper;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final DiscountCardAppendDistributorIdForListService discountCardAppendDistributorIdForListService;
	private final DistributorCouponListAppendService distributorCouponListAppendService;

	public UserDiscountMyUserCardListService(
			UserDiscountMapper userDiscountMapper,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			DiscountCardAppendDistributorIdForListService discountCardAppendDistributorIdForListService,
			DistributorCouponListAppendService distributorCouponListAppendService) {
		this.userDiscountMapper = userDiscountMapper;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.discountCardAppendDistributorIdForListService = discountCardAppendDistributorIdForListService;
		this.distributorCouponListAppendService = distributorCouponListAppendService;
	}

	public Map<String, Object> build(
			long companyId,
			long userId,
			String mobile,
			String status,
			String cardType,
			String scopeType,
			String sourceType,
			String sourceIdParam,
			int page,
			int pageSize) {
		Objects.requireNonNull(mobile, "mobile");
		int ps = pageSize > 50 ? 50 : (pageSize <= 0 ? 20 : pageSize);
		int pn = page < 1 ? 1 : page;
		int nowEpoch = (int) (System.currentTimeMillis() / 1000L);

		UserDiscountMyCardListFilterParams f = buildFilterParams(
				companyId, userId, nowEpoch, status, cardType, scopeType, sourceType, sourceIdParam);

		long total = userDiscountMapper.countMyUserCardListDistinct(f);
		if (total == 0L) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", List.of());
			empty.put("total_count", 0L);
			return empty;
		}

		long offset = (long) (pn - 1) * ps;
		List<Map<String, Object>> rows = userDiscountMapper.selectMyUserCardListPage(f, offset, ps);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			list.add(decorateRow(row, nowEpoch));
		}

		UserDiscountMyCardListFilterParams countBase = copyForCount(f);
		countBase.setCardTypeFilter(null);
		Map<String, Object> count = new LinkedHashMap<>();
		count.put("total", userDiscountMapper.countMyUserCardListDistinct(countBase));
		countBase.setCardTypeFilter("discount");
		count.put("discount", userDiscountMapper.countMyUserCardListDistinct(countBase));
		countBase.setCardTypeFilter("cash");
		count.put("cash", userDiscountMapper.countMyUserCardListDistinct(countBase));
		countBase.setCardTypeFilter("new_gift");
		count.put("new_gift", userDiscountMapper.countMyUserCardListDistinct(countBase));

		CurrencyExchangeRate rate = companyDefaultCurrencyService.getCur(companyId);
		Map<String, Object> cur = currencyToCurMap(rate);

		discountCardAppendDistributorIdForListService.apply(companyId, list);
		distributorCouponListAppendService.appendDistributorInfo(companyId, list);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", total);
		out.put("count", count);
		out.put("cur", cur);
		return out;
	}

	private UserDiscountMyCardListFilterParams buildFilterParams(
			long companyId,
			long userId,
			int nowEpoch,
			String status,
			String cardType,
			String scopeType,
			String sourceType,
			String sourceIdParam) {
		UserDiscountMyCardListFilterParams f = new UserDiscountMyCardListFilterParams();
		f.setCompanyId(companyId);
		f.setUserId(userId);
		f.setNowEpoch(nowEpoch);
		f.setStatusFilterMode(resolveStatusFilterMode(status));
		f.setScopeTypeAll(Boolean.valueOf("all".equals(scopeType)));
		f.setDistributorId(0L);
		f.setProductModel(null);
		if (ValuePresence.hasEffectiveValue(cardType)) {
			f.setCardTypeFilter(cardType.trim());
		}
		if (ValuePresence.hasEffectiveValue(sourceType)) {
			f.setDiscountCardSourceType(sourceType.trim());
		}
		Long sid = parsePositiveLongOrNull(sourceIdParam);
		if (sid != null) {
			f.setDiscountCardSourceId(sid);
		}
		return f;
	}

	private static String resolveStatusFilterMode(String status) {
		if (status == null) {
			return "NONE";
		}
		return switch (status) {
			case "1" -> "TAB1";
			case "2" -> "TAB2";
			case "3" -> "TAB3";
			default -> "NONE";
		};
	}

	private static Long parsePositiveLongOrNull(String raw) {
		if (!ValuePresence.hasEffectiveValue(raw)) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static UserDiscountMyCardListFilterParams copyForCount(UserDiscountMyCardListFilterParams src) {
		UserDiscountMyCardListFilterParams c = new UserDiscountMyCardListFilterParams();
		c.setCompanyId(src.getCompanyId());
		c.setUserId(src.getUserId());
		c.setNowEpoch(src.getNowEpoch());
		c.setStatusFilterMode(src.getStatusFilterMode());
		c.setScopeTypeAll(src.getScopeTypeAll());
		c.setDistributorId(src.getDistributorId());
		c.setProductModel(src.getProductModel());
		c.setCardTypeFilter(src.getCardTypeFilter());
		c.setDiscountCardSourceType(src.getDiscountCardSourceType());
		c.setDiscountCardSourceId(src.getDiscountCardSourceId());
		return c;
	}

	private Map<String, Object> decorateRow(Map<String, Object> row, int nowEpoch) {
		Map<String, Object> item = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			item.put(camelToSnakeKey(e.getKey()), e.getValue());
		}

		Integer beginEpoch = intFromRow(row, "beginDate", "begin_date");
		Integer endEpoch = intFromRow(row, "endDate", "end_date");
		if (beginEpoch != null) {
			item.put("begin_date", formatYmd(beginEpoch));
		}
		if (endEpoch != null) {
			item.put("end_date", formatYmd(endEpoch));
		}

		Integer st = intFromRow(row, "status", "status");
		if (st != null && st == 2) {
			item.put("tagClass", "used");
		} else {
			if (beginEpoch != null && endEpoch != null && beginEpoch > nowEpoch && endEpoch > nowEpoch) {
				item.put("tagClass", "notstarted");
			} else if (endEpoch != null && endEpoch < nowEpoch) {
				item.put("tagClass", "overdue");
			}
		}

		String ct = strFromRow(row, "cardType", "card_type");
		Map<String, Object> coupon = new LinkedHashMap<>();
		Long cardIdVal = longFromRowAllowZero(row, "cardId", "card_id");
		if (cardIdVal != null) {
			coupon.put("card_id", cardIdVal);
		}
		Object title = firstNonNullRow(row, "title");
		coupon.put("title", title != null ? title.toString() : "");
		Object code = firstNonNullRow(row, "code");
		coupon.put("code", code != null ? code.toString() : "");
		coupon.put("card_type", ct != null ? ct : "");
		if ("cash".equals(ct)) {
			Integer lc = intFromRow(row, "leastCost", "least_cost");
			Integer rc = intFromRow(row, "reduceCost", "reduce_cost");
			if (lc != null) {
				coupon.put("least_cost", lc);
			}
			if (rc != null) {
				coupon.put("reduce_cost", rc);
			}
		} else if ("discount".equals(ct)) {
			Integer disc = intFromRow(row, "discount", "discount");
			if (disc != null) {
				coupon.put("discount", disc);
			}
		}
		item.put("coupon", coupon);

		Object cardTemplateSourceType = firstNonNullRow(row, "cardTemplateSourceType", "card_template_source_type");
		if (cardTemplateSourceType != null) {
			item.put("source_type", cardTemplateSourceType.toString());
		}
		item.remove("card_template_source_type");

		return item;
	}

	private static String camelToSnakeKey(String k) {
		if (k == null || k.isEmpty()) {
			return k;
		}
		if (k.indexOf('_') >= 0) {
			return k;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < k.length(); i++) {
			char c = k.charAt(i);
			if (Character.isUpperCase(c) && i > 0) {
				sb.append('_');
			}
			sb.append(Character.toLowerCase(c));
		}
		return sb.toString();
	}

	private static Object firstNonNullRow(Map<String, Object> row, String... keys) {
		for (String key : keys) {
			if (row.containsKey(key) && row.get(key) != null) {
				return row.get(key);
			}
		}
		return null;
	}

	private static Integer intFromRow(Map<String, Object> row, String camel, String snake) {
		Object v = row.get(camel);
		if (v == null) {
			v = row.get(snake);
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long longFromRowAllowZero(Map<String, Object> row, String camel, String snake) {
		Object v = row.get(camel);
		if (v == null) {
			v = row.get(snake);
		}
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

	private static String strFromRow(Map<String, Object> row, String camel, String snake) {
		Object v = row.get(camel);
		if (v == null) {
			v = row.get(snake);
		}
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private static String formatYmd(int epochSeconds) {
		return YMD.format(Instant.ofEpochSecond(epochSeconds));
	}

	private static Map<String, Object> currencyToCurMap(CurrencyExchangeRate r) {
		Map<String, Object> m = new LinkedHashMap<>();
		putIfNonNull(m, "id", r.getId());
		putIfNonNull(m, "companyId", r.getCompanyId());
		putIfNonNull(m, "title", r.getTitle());
		putIfNonNull(m, "currency", r.getCurrency());
		putIfNonNull(m, "symbol", r.getSymbol());
		putIfNonNull(m, "rate", r.getRate());
		putIfNonNull(m, "isDefault", r.getIsDefault());
		putIfNonNull(m, "usePlatform", r.getUsePlatform());
		return m;
	}

	private static void putIfNonNull(Map<String, Object> m, String key, Object val) {
		if (val != null) {
			m.put(key, val);
		}
	}
}
