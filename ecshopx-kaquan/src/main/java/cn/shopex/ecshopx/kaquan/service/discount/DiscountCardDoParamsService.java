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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardDoParamsService {

	private final DiscountCardCodeUniquenessService codeUniquenessService;

	public DiscountCardDoParamsService(DiscountCardCodeUniquenessService codeUniquenessService) {
		this.codeUniquenessService = codeUniquenessService;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> apply(Map<String, Object> postdata, long companyId) {
		Map<String, Object> p = new HashMap<>(postdata);
		String cardType = DiscountCardParamNormalize.stringVal(p.get("card_type"));
		if (!"gift".equals(cardType)) {
			p.put("use_platform", "mall");
			p.put("use_scenes", "ONLINE");
		}
		int useCondition = DiscountCardParamNormalize.parseIntFlexible(p.get("useCondition"), 0);
		if ("cash".equals(cardType) && useCondition == 2 && p.containsKey("least_cost") && p.containsKey("reduce_cost")) {
			int least = DiscountCardParamNormalize.parseIntFlexible(p.get("least_cost"), 0);
			int reduce = DiscountCardParamNormalize.parseIntFlexible(p.get("reduce_cost"), 0);
			if (least <= reduce) {
				throw new ResourceException(KaquanDiscountCardMessages.CASH_REDUCE_MIN);
			}
		}
		if ("discount".equals(cardType) && useCondition == 2 && p.containsKey("least_cost") && p.containsKey("most_cost")) {
			if (!DiscountCardParamNormalize.isNumericString(p.get("least_cost"))) {
				throw new ResourceException(KaquanDiscountCardMessages.DISCOUNT_MIN_COST_NUMERIC);
			}
			if (!DiscountCardParamNormalize.isNumericString(p.get("most_cost"))) {
				throw new ResourceException(KaquanDiscountCardMessages.DISCOUNT_MAX_LIMIT_NUMERIC);
			}
			int least = DiscountCardParamNormalize.parseIntFlexible(p.get("least_cost"), 0);
			int most = DiscountCardParamNormalize.parseIntFlexible(p.get("most_cost"), 0);
			if (least < 0) {
				throw new ResourceException(KaquanDiscountCardMessages.DISCOUNT_MIN_COST_GTE_ZERO);
			}
			if (least >= most) {
				throw new ResourceException(KaquanDiscountCardMessages.DISCOUNT_MAX_LIMIT_GT_MIN);
			}
		}
		if ("money".equals(cardType) && useCondition == 2 && p.containsKey("least_cost") && p.containsKey("reduce_cost")) {
			int least = DiscountCardParamNormalize.parseIntFlexible(p.get("least_cost"), 0);
			int reduce = DiscountCardParamNormalize.parseIntFlexible(p.get("reduce_cost"), 0);
			if (least <= reduce) {
				throw new ResourceException(KaquanDiscountCardMessages.MONEY_REDUCE_MIN);
			}
		}
		String useScenes = DiscountCardParamNormalize.stringVal(p.get("use_scenes"));
		if (StringUtils.hasText(useScenes)) {
			if ("SELF".equals(useScenes) && p.containsKey("self_consume_code")) {
				p.put("self_consume_code", DiscountCardParamNormalize.parseIntFlexible(p.get("self_consume_code"), 0));
			} else {
				p.put("self_consume_code", 0);
			}
		}
		String useAllShops = DiscountCardParamNormalize.stringVal(p.get("use_all_shops"));
		if ("false".equalsIgnoreCase(useAllShops)) {
			String usePlatform = DiscountCardParamNormalize.stringVal(p.get("use_platform"));
			if ("store".equals(usePlatform) && isEmptyRelShops(p)) {
				throw new ResourceException(KaquanDiscountCardMessages.APPLICABLE_STORES_REQUIRED);
			}
			if ("mall".equals(usePlatform)) {
				Object distIds = p.get("distributor_ids");
				if (distIds != null && isEmptyCollectionOrString(distIds)) {
					throw new ResourceException(KaquanDiscountCardMessages.APPLICABLE_SHOPS_REQUIRED);
				}
			}
		}
		List<String> distributorFromIds = new ArrayList<>();
		Object rawDist = p.get("distributor_ids");
		if (rawDist instanceof List<?> l) {
			for (Object o : l) {
				if (o != null && StringUtils.hasText(String.valueOf(o).trim())) {
					distributorFromIds.add(String.valueOf(o).trim());
				}
			}
		}
		p.put("distributor_id", distributorFromIds);
		Object timeLimitType = p.get("time_limit_type");
		if (timeLimitType instanceof List<?> types) {
			List<Map<String, Object>> built = new ArrayList<>();
			Object datesRaw = p.get("time_limit_date");
			for (Object tv : types) {
				String typeVal = String.valueOf(tv);
				if (datesRaw instanceof List<?> dates) {
					for (Object d : dates) {
						if (d instanceof Map<?, ?> dm) {
							Map<String, Object> day = (Map<String, Object>) dm;
							Object bt = day.get("begin_time");
							Object et = day.get("end_time");
							if (bt != null && et != null) {
								String[] b = String.valueOf(bt).split(":");
								String[] e = String.valueOf(et).split(":");
								Map<String, Object> row = new HashMap<>();
								row.put("type", typeVal);
								row.put("begin_hour", Integer.parseInt(b[0].trim()));
								row.put("begin_minute", b.length > 1 ? Integer.parseInt(b[1].trim()) : 0);
								row.put("end_hour", Integer.parseInt(e[0].trim()));
								row.put("end_minute", e.length > 1 ? Integer.parseInt(e[1].trim()) : 0);
								built.add(row);
							}
						}
					}
				} else {
					Map<String, Object> row = new HashMap<>();
					row.put("type", typeVal);
					built.add(row);
				}
			}
			p.put("time_limit", built);
			p.remove("time_limit_type");
			p.remove("time_limit_date");
		}
		if ("DATE_TYPE_FIX_TIME_RANGE".equals(DiscountCardParamNormalize.stringVal(p.get("date_type"))) && p.get("begin_time") != null) {
			long begin = DiscountCardParamNormalize.longFromObject(p.get("begin_time"), 0L);
			long end = DiscountCardParamNormalize.longFromObject(p.get("end_time"), 0L);
			long now = System.currentTimeMillis() / 1000L;
			Long cardId = parseLongOrNull(p.get("card_id"));
			if (cardId == null && end < now) {
				throw new ResourceException(KaquanDiscountCardMessages.END_TIME_GT_TODAY);
			}
			if (begin > end) {
				throw new ResourceException(KaquanDiscountCardMessages.END_TIME_GT_START_TIME);
			}
		}
		int beginTime = DiscountCardParamNormalize.parseIntFlexible(p.get("begin_time"), 0);
		p.put("begin_time", beginTime != 0 ? beginTime : 0);
		if (p.get("end_time") != null && DiscountCardParamNormalize.parseIntFlexible(p.get("end_time"), 0) != 0) {
			long end = DiscountCardParamNormalize.longFromObject(p.get("end_time"), 0L);
			long now = System.currentTimeMillis() / 1000L;
			Long cardId = parseLongOrNull(p.get("card_id"));
			if (cardId == null && end < now) {
				throw new ResourceException(KaquanDiscountCardMessages.END_DATE_EXPIRED);
			}
		} else {
			p.put("end_time", 0);
		}
		int quantity = DiscountCardParamNormalize.parseIntFlexible(p.get("quantity"), 0);
		if (quantity == 0) {
			throw new ResourceException(KaquanDiscountCardMessages.QUANTITY_MIN_ONE);
		}
		Object cardCode = p.get("card_code");
		if (cardCode != null && StringUtils.hasText(String.valueOf(cardCode).trim())) {
			String rule = DiscountCardParamNormalize.stringVal(p.get("card_rule_code"));
			Long excludeId = parseLongOrNull(p.get("card_id"));
			codeUniquenessService.assertCardCodeAvailable(companyId, String.valueOf(cardCode).trim(), rule, excludeId);
		}
		if (!p.containsKey("card_rule_code")) {
			p.put("card_rule_code", "");
		}
		int getLimit = DiscountCardParamNormalize.parseIntFlexible(p.get("get_limit"), 1);
		if (p.containsKey("get_limit") && getLimit <= 0) {
			p.put("get_limit", 1);
		}
		if ("forbid".equals(DiscountCardParamNormalize.stringVal(p.get("use_all_items")))) {
			p.put("use_all_items", "false");
		}
		return p;
	}

	private static Long parseLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		String s = String.valueOf(o).trim();
		if (!org.springframework.util.StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isEmptyRelShops(Map<String, Object> p) {
		Object v = p.get("rel_shops_ids");
		if (v == null) {
			return true;
		}
		if (v instanceof List<?> l) {
			return l.isEmpty();
		}
		String s = String.valueOf(v).trim();
		return !StringUtils.hasText(s);
	}

	private static boolean isEmptyCollectionOrString(Object v) {
		if (v instanceof List<?> l) {
			return l.isEmpty();
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return v == null;
	}
}
