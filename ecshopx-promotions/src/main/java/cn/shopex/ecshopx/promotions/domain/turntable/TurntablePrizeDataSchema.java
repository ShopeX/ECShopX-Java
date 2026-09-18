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
import org.springframework.util.StringUtils;

/**
 * prize_data[] 元素契约校验（PRD §13.1）。BE-01 锁住 schema 不变量；完整保存流程在 BE-02 接入。
 */
public final class TurntablePrizeDataSchema {

	public static final int NAME_MAX_CHARS = 5;
	public static final int PROBABILITY_MIN = 0;
	public static final int PROBABILITY_MAX = 100;

	private TurntablePrizeDataSchema() {}

	public static List<String> validatePrizeList(List<Map<String, Object>> prizes) {
		List<String> errors = new ArrayList<>();
		if (prizes == null || prizes.isEmpty()) {
			errors.add("至少配置一个奖项");
			return errors;
		}
		boolean hasThanks = false;
		int probabilitySum = 0;
		for (int i = 0; i < prizes.size(); i++) {
			Map<String, Object> prize = prizes.get(i) == null ? Map.of() : prizes.get(i);
			String prefix = "奖项[" + i + "]";
			String prizeId = asTrimmedString(prize.get("prize_id"));
			if (!StringUtils.hasText(prizeId)) {
				errors.add(prefix + ": prize_id 必填");
			}
			Integer sort = asInteger(prize.get("sort"));
			if (sort == null) {
				errors.add(prefix + ": sort 必填");
			}
			String name = asTrimmedString(prize.get("name"));
			if (!StringUtils.hasText(name)) {
				errors.add(prefix + ": name 必填");
			} else if (name.codePointCount(0, name.length()) > NAME_MAX_CHARS) {
				errors.add(prefix + ": name 不超过" + NAME_MAX_CHARS + "字");
			}
			String type = asTrimmedString(prize.get("type"));
			if (!isKnownType(type)) {
				errors.add(prefix + ": type 非法");
			} else if (TurntablePrizeType.THANKS.equals(type)) {
				hasThanks = true;
			}
			Integer probability = asInteger(prize.get("probability"));
			if (probability == null) {
				errors.add(prefix + ": probability 必填");
			} else if (probability < PROBABILITY_MIN || probability > PROBABILITY_MAX) {
				errors.add(prefix + ": probability 须在 0～100");
			} else {
				probabilitySum += probability;
			}
			if (TurntablePrizeType.POINTS.equals(type)) {
				Long points = asLong(prize.get("value"));
				if (points == null || points <= 0L) {
					errors.add(prefix + ": points 的 value 必须大于 0");
				}
			}
			if (TurntablePrizeType.COUPON.equals(type) || TurntablePrizeType.COUPONS.equals(type)) {
				if (!StringUtils.hasText(asTrimmedString(prize.get("value")))) {
					errors.add(prefix + ": coupon/coupons 的 value 必填");
				}
				Integer dailyStock = asInteger(firstPresent(prize, "dailyStock", "daily_stock", "stock"));
				if (dailyStock == null) {
					errors.add(prefix + ": dailyStock 必填");
				} else if (dailyStock < 0) {
					errors.add(prefix + ": dailyStock 须为非负整数");
				}
			}
		}
		if (!hasThanks) {
			errors.add("必须至少配置一个 thanks");
		}
		if (probabilitySum > PROBABILITY_MAX) {
			errors.add("概率之和不得大于 100");
		}
		return errors;
	}

	public static boolean isValidPrizeList(List<Map<String, Object>> prizes) {
		return validatePrizeList(prizes).isEmpty();
	}

	/** 奖项 schema 中「未填写必填项」类错误（与格式/范围类错误区分）。 */
	public static boolean isRequiredFieldError(String error) {
		if (!StringUtils.hasText(error)) {
			return false;
		}
		return error.contains("必填")
				|| error.contains("至少配置")
				|| error.contains("必须至少配置");
	}

	/** 0 = 不限制（次数上限语义）。 */
	public static boolean isUnlimitedCount(long limit) {
		return limit == 0L;
	}

	/** coupon/coupons：dailyStock=0 表示今日不可中。 */
	public static boolean isDailyStockBlockedToday(int dailyStock) {
		return dailyStock == 0;
	}

	private static boolean isKnownType(String type) {
		return TurntablePrizeType.THANKS.equals(type)
				|| TurntablePrizeType.POINTS.equals(type)
				|| TurntablePrizeType.COUPON.equals(type)
				|| TurntablePrizeType.COUPONS.equals(type);
	}

	private static Object firstPresent(Map<String, Object> map, String... keys) {
		for (String key : keys) {
			if (map.containsKey(key) && map.get(key) != null) {
				return map.get(key);
			}
		}
		return null;
	}

	private static String asTrimmedString(Object raw) {
		if (raw == null) {
			return "";
		}
		return Objects.toString(raw, "").trim();
	}

	private static Integer asInteger(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long asLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static Map<String, Object> sampleThanks(String prizeId, int sort, int probability) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("prize_id", prizeId);
		m.put("sort", sort);
		m.put("name", "谢谢");
		m.put("type", TurntablePrizeType.THANKS);
		m.put("value", null);
		m.put("probability", probability);
		return m;
	}

	public static Map<String, Object> sampleCoupon(
			String prizeId, int sort, int probability, String couponId, int dailyStock) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("prize_id", prizeId);
		m.put("sort", sort);
		m.put("name", "券");
		m.put("type", TurntablePrizeType.COUPON);
		m.put("value", couponId);
		m.put("probability", probability);
		m.put("dailyStock", dailyStock);
		return m;
	}
}
