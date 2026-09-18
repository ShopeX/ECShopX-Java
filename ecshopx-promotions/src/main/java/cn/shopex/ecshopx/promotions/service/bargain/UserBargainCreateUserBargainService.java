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

package cn.shopex.ecshopx.promotions.service.bargain;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserBargainCreateUserBargainService {

	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final UserBargainsMapper userBargainsMapper;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public UserBargainCreateUserBargainService(
			BargainPromotionsMapper bargainPromotionsMapper,
			UserBargainsMapper userBargainsMapper,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.userBargainsMapper = userBargainsMapper;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createUserBargain(
			long companyId,
			long userId,
			String authorizerAppid,
			String wxaAppid,
			String bargainIdRaw,
			Locale locale) {
		Long bargainIdForQuery = resolveBargainIdForQuery(bargainIdRaw);

		BargainPromotions promotion;
		if (bargainIdForQuery == null) {
			promotion = null;
		} else {
			LambdaQueryWrapper<BargainPromotions> w =
					new LambdaQueryWrapper<BargainPromotions>()
							.eq(BargainPromotions::getBargainId, bargainIdForQuery)
							.last("LIMIT 1");
			promotion = bargainPromotionsMapper.selectOne(w);
		}
		if (promotion == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_bargain_id",
							new Object[] {bargainIdRaw == null ? "" : bargainIdRaw},
							locale));
		}

		long now = Instant.now().getEpochSecond();
		Long end = promotion.getEndTime();
		if (end != null && end < now) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.activity_ended_expect_next_participation", null, locale));
		}
		Long begin = promotion.getBeginTime();
		if (begin != null && begin > now) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.activity_not_started_please_wait", null, locale));
		}

		LambdaQueryWrapper<UserBargains> ub =
				new LambdaQueryWrapper<UserBargains>()
						.eq(UserBargains::getCompanyId, companyId)
						.eq(UserBargains::getUserId, userId)
						.eq(UserBargains::getBargainId, bargainIdForQuery)
						.last("LIMIT 1");
		UserBargains existing = userBargainsMapper.selectOne(ub);
		if (existing != null) {
			return toUserBargainRow(existing, locale);
		}

		String prJson = promotion.getPeopleRange();
		if (!StringUtils.hasText(prJson)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.people_range_invalid", null, locale));
		}
		Map<String, Object> prMap;
		try {
			prMap = objectMapper.readValue(prJson, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.people_range_invalid", null, locale));
		}
		int minPeople = parsePositiveIntFlexible(prMap.get("min"));
		int maxPeople = parsePositiveIntFlexible(prMap.get("max"));
		if (minPeople <= 0 || maxPeople <= 0 || maxPeople < minPeople) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.people_range_invalid", null, locale));
		}

		int num = ThreadLocalRandom.current().nextInt(minPeople, maxPeople + 1);

		int mkt = nz(promotion.getMktPrice());
		int price = nz(promotion.getPrice());
		int total = mkt - price;
		int minCut = -(int) Math.floor((total / (double) num) / 2.0);

		List<Map<String, Object>> slots = getRandPriceNum(total, num, minCut);

		UserBargains row = new UserBargains();
		row.setBargainId(bargainIdForQuery);
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setAuthorizerAppid(authorizerAppid == null ? "" : authorizerAppid);
		row.setWxaAppid(wxaAppid == null ? "" : wxaAppid);
		row.setItemName(promotion.getItemName());
		row.setMktPrice(promotion.getMktPrice());
		row.setPrice(promotion.getPrice());
		row.setCutpriceNum(num);
		try {
			row.setCutpriceRange(objectMapper.writeValueAsString(slots));
		} catch (JsonProcessingException e) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
		}
		row.setCutdownAmount(0);
		row.setIsOrdered(Boolean.FALSE);
		int nowInt = (int) Math.min(now, Integer.MAX_VALUE);
		row.setCreated(nowInt);
		row.setUpdated(nowInt);

		try {
			userBargainsMapper.insert(row);
		} catch (DataIntegrityViolationException e) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_bargain_id",
							new Object[] {String.valueOf(bargainIdForQuery)},
							locale));
		}

		UserBargains loaded = userBargainsMapper.selectOne(ub);
		if (loaded == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_bargain_id",
							new Object[] {String.valueOf(bargainIdForQuery)},
							locale));
		}
		return toUserBargainRow(loaded, locale);
	}

	private Map<String, Object> toUserBargainRow(UserBargains e, Locale locale) {
		String cr = e.getCutpriceRange();
		List<Map<String, Object>> cutpriceRangeParsed;
		if (!StringUtils.hasText(cr)) {
			cutpriceRangeParsed = List.of();
		} else {
			try {
				cutpriceRangeParsed =
						objectMapper.readValue(cr, new TypeReference<List<Map<String, Object>>>() {});
			} catch (JsonProcessingException ex) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.bargain.bargain_activity_completed_cannot_help", null, locale));
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", longIdToString(e.getCompanyId()));
		out.put("authorizer_appid", nzAppId(e.getAuthorizerAppid()));
		out.put("wxa_appid", nzAppId(e.getWxaAppid()));
		out.put("bargain_id", longIdToString(e.getBargainId()));
		out.put("user_id", longIdToString(e.getUserId()));
		out.put("item_name", e.getItemName() == null ? "" : e.getItemName());
		out.put("mkt_price", String.valueOf(nz(e.getMktPrice())));
		out.put("price", String.valueOf(nz(e.getPrice())));
		out.put("cutprice_num", String.valueOf(nz(e.getCutpriceNum())));
		out.put("cutprice_range", cutpriceRangeParsed);
		out.put("cutdown_amount", String.valueOf(nz(e.getCutdownAmount())));
		out.put(
				"is_ordered",
				Boolean.TRUE.equals(e.getIsOrdered()) ? Boolean.TRUE : Boolean.FALSE);
		out.put("created", intTimestampToString(e.getCreated()));
		out.put("updated", intTimestampToString(e.getUpdated()));
		return out;
	}

	private static String longIdToString(Long v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static String nzAppId(String v) {
		return StringUtils.hasText(v) ? v : "";
	}

	private static String intTimestampToString(Integer v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static Long resolveBargainIdForQuery(String bargainIdRaw) {
		if (bargainIdRaw == null || bargainIdRaw.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(bargainIdRaw);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parsePositiveIntFlexible(Object raw) {
		if (raw == null) {
			return 0;
		}
		try {
			if (raw instanceof Number n) {
				int v = n.intValue();
				return v > 0 ? v : 0;
			}
			String s = String.valueOf(raw).trim();
			if (s.isEmpty()) {
				return 0;
			}
			int v = Integer.parseInt(s);
			return v > 0 ? v : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

	private static List<Map<String, Object>> getRandPriceNum(int total, int num, int min) {
		if (num == 1) {
			List<Map<String, Object>> one = new ArrayList<>(1);
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("cut", total);
			m.put("used", 0);
			one.add(m);
			return one;
		}
		List<Integer> result = new ArrayList<>();
		int i = 1;
		int money;
		int resultTotal = 0;
		int tmptotal = total;
		int totalRem = total;
		while (true) {
			int denom = num - i;
			double safeTotalD = (totalRem - (long) denom * min) / (double) denom;
			int safeTotal = (int) safeTotalD;
			int lo = Math.min(min, safeTotal);
			int hi = Math.max(min, safeTotal);
			if (lo > hi) {
				money = lo;
			} else {
				money = ThreadLocalRandom.current().nextInt(lo, hi + 1);
			}
			if (i == 1 && money < 0) {
				continue;
			}
			totalRem = totalRem - money;
			resultTotal += money;
			if (resultTotal > 0 && totalRem > 0 && tmptotal > money) {
				result.add(money);
				i++;
			} else {
				totalRem += money;
			}
			if (i >= num) {
				break;
			}
		}
		result.add(totalRem);
		List<Map<String, Object>> data = new ArrayList<>(result.size());
		for (int v : result) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("cut", v);
			row.put("used", 0);
			data.add(row);
		}
		return data;
	}
}
