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

import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserDiscountUserRecordDetailAssembler {

	private final ObjectMapper objectMapper;

	public UserDiscountUserRecordDetailAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toDetailMap(UserDiscount ud) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", ud.getId());
		m.put("user_id", ud.getUserId());
		m.put("company_id", ud.getCompanyId());
		m.put("card_id", ud.getCardId());
		m.put("code", ud.getCode());
		m.put("status", ud.getStatus());
		m.put("card_type", ud.getCardType());
		m.put("begin_date", ud.getBeginDate());
		m.put("end_date", ud.getEndDate());
		m.put("title", ud.getTitle());
		m.put("discount", ud.getDiscount());
		m.put("least_cost", ud.getLeastCost());
		m.put("reduce_cost", ud.getReduceCost());
		m.put("use_scenes", ud.getUseScenes());
		m.put("rel_shops_ids", normalizeRelShopsIds(ud.getRelShopsIds()));
		m.put("rel_distributor_ids", splitCommaStringList(ud.getRelDistributorIds()));
		m.put("rel_item_ids", normalizeRelItemIds(ud.getRelItemIds()));
		m.put("use_condition", parseUseConditionJson(ud.getUseCondition()));
		m.put("is_valid", computeIsValid(ud));
		return m;
	}

	private boolean computeIsValid(UserDiscount ud) {
		long nowEpoch = System.currentTimeMillis() / 1000L;
		Integer st = ud.getStatus();
		if (st == null || st != 1) {
			return false;
		}
		Integer begin = ud.getBeginDate();
		if (begin != null && begin > nowEpoch) {
			return false;
		}
		Integer end = ud.getEndDate();
		if (end != null && end <= nowEpoch) {
			return false;
		}
		return true;
	}

	private static Object normalizeRelShopsIds(String raw) {
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return "all";
		}
		return splitCommaLongTokens(raw);
	}

	private static Object normalizeRelItemIds(String raw) {
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return "all";
		}
		return splitCommaStringList(raw);
	}

	private static List<String> splitCommaStringList(String raw) {
		List<String> list = new ArrayList<>();
		if (!StringUtils.hasText(raw)) {
			return list;
		}
		for (String p : raw.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				list.add(p.trim());
			}
		}
		return list;
	}

	private static List<Long> splitCommaLongTokens(String raw) {
		List<Long> list = new ArrayList<>();
		for (String s : splitCommaStringList(raw)) {
			try {
				long v = Long.parseLong(s);
				if (v > 0L) {
					list.add(v);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return list;
	}

	private Object parseUseConditionJson(String raw) {
		if (!StringUtils.hasText(raw)) {
			return raw;
		}
		try {
			return objectMapper.readValue(raw.trim(), Object.class);
		} catch (JsonProcessingException e) {
			return raw;
		}
	}
}
