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

package cn.shopex.ecshopx.promotions.domain.turntable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * 管理端保存侧：奖项字段归一化、prize_id、次数校验、结束态判断（PRD §4 / §4.5）。
 */
public final class TurntableAdminSaveRules {

	public static final String ERR_CONFIG_VERSION_CONFLICT = "LUCKY_DRAW_CONFIG_VERSION_CONFLICT";
	public static final String ERR_ENDED_READONLY = "LUCKY_DRAW_ENDED";
	public static final String I18N_REQUIRED_FIELDS_CANNOT_BE_EMPTY =
			"promotions.turntable.required_fields_cannot_be_empty";
	public static final String FALLBACK_REQUIRED_FIELDS_CANNOT_BE_EMPTY = "请检查配置，必填项不能为空";
	public static final long COST_TYPE_POINT = 2L;

	private TurntableAdminSaveRules() {}

	public static boolean isEnded(long beginTime, long endTime, long nowEpochSec) {
		return nowEpochSec >= endTime;
	}

	public static boolean isInProgress(long beginTime, long endTime, long nowEpochSec) {
		return beginTime <= nowEpochSec && nowEpochSec < endTime;
	}

	/** 普通保存：end_time 必须严格大于 now。 */
	public static boolean isEndTimeValidForNormalSave(long endTime, long nowEpochSec) {
		return endTime > nowEpochSec;
	}

	public static List<String> validateLimits(Long limitTotal, Long limitDay) {
		List<String> errors = new ArrayList<>();
		if (limitTotal == null) {
			errors.add("limit_total 必填");
		} else if (limitTotal < 0L) {
			errors.add("limit_total 须为非负整数");
		}
		if (limitDay == null) {
			errors.add("limit_day 必填");
		} else if (limitDay < 0L) {
			errors.add("limit_day 须为非负整数");
		}
		if (limitTotal != null && limitDay != null && limitTotal > 0L && limitDay > limitTotal) {
			errors.add("每日次数不得大于总次数");
		}
		return errors;
	}

	/**
	 * 将现网旧字段名归一到 PRD §13.1；生成/保留 prize_id；补 sort。
	 *
	 * @param existingPrizeIdsByIndex 编辑时按索引保留的旧 prize_id（可空）
	 */
	public static List<Map<String, Object>> normalizeAndAssignPrizeIds(
			List<Map<String, Object>> rawPrizes, List<String> existingPrizeIdsByIndex) {
		List<Map<String, Object>> out = new ArrayList<>();
		if (rawPrizes == null) {
			return out;
		}
		for (int i = 0; i < rawPrizes.size(); i++) {
			Map<String, Object> raw = rawPrizes.get(i) == null ? Map.of() : rawPrizes.get(i);
			Map<String, Object> n = new LinkedHashMap<>();
			String existingId =
					existingPrizeIdsByIndex != null && i < existingPrizeIdsByIndex.size()
							? existingPrizeIdsByIndex.get(i)
							: null;
			String prizeId = firstNonBlank(asText(raw.get("prize_id")), existingId);
			if (!StringUtils.hasText(prizeId)) {
				prizeId = UUID.randomUUID().toString().replace("-", "");
			}
			n.put("prize_id", prizeId);
			n.put("sort", firstInt(raw.get("sort"), i + 1));
			n.put("name", firstNonBlank(asText(raw.get("name")), asText(raw.get("prize_title")), "奖项"));
			String type =
					firstNonBlank(asText(raw.get("type")), asText(raw.get("prize_type"))).toLowerCase();
			n.put("type", type);
			Object value = raw.containsKey("value") ? raw.get("value") : raw.get("prize_value");
			n.put("value", value);
			n.put(
					"probability",
					firstInt(
							raw.get("probability"),
							firstInt(raw.get("prize_probability"), 0)));
			Object daily =
					firstPresent(raw, "dailyStock", "daily_stock", "stock");
			if (daily != null) {
				n.put("dailyStock", daily);
			}
			if (raw.containsKey("backgroundColor")) {
				n.put("backgroundColor", raw.get("backgroundColor"));
			}
			if (raw.containsKey("image")) {
				n.put("image", raw.get("image"));
			}
			// 兼容旧前端：同步写回旧键，避免读侧瞬时空
			n.put("prize_type", type);
			n.put("prize_probability", n.get("probability"));
			n.put("prize_value", value);
			if (n.containsKey("dailyStock")) {
				n.put("stock", n.get("dailyStock"));
			}
			out.add(n);
		}
		return out;
	}

	/** 复制活动：全部 prize_id 重新生成。 */
	public static List<Map<String, Object>> regenerateAllPrizeIds(List<Map<String, Object>> prizes) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> p : prizes == null ? List.<Map<String, Object>>of() : prizes) {
			Map<String, Object> copy = new LinkedHashMap<>(p);
			copy.put("prize_id", UUID.randomUUID().toString().replace("-", ""));
			out.add(copy);
		}
		return out;
	}

	public static List<String> extractPrizeIdsInOrder(String prizeDataJson, com.fasterxml.jackson.databind.ObjectMapper om) {
		if (!StringUtils.hasText(prizeDataJson)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> list =
					om.readValue(prizeDataJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
			List<String> ids = new ArrayList<>();
			for (Map<String, Object> m : list) {
				ids.add(asText(m.get("prize_id")));
			}
			return ids;
		} catch (Exception e) {
			return List.of();
		}
	}

	private static Object firstPresent(Map<String, Object> map, String... keys) {
		for (String key : keys) {
			if (map.containsKey(key) && map.get(key) != null) {
				return map.get(key);
			}
		}
		return null;
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) {
			if (StringUtils.hasText(v)) {
				return v.trim();
			}
		}
		return "";
	}

	private static String asText(Object raw) {
		return raw == null ? "" : Objects.toString(raw, "").trim();
	}

	private static int firstInt(Object primary, int fallback) {
		Integer v = asInteger(primary);
		return v == null ? fallback : v;
	}

	private static Integer asInteger(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
