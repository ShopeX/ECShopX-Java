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
 * C 端公开配置：脱敏概率/库存、剩余次数语义（PRD §7）。
 */
public final class TurntablePublicConfigRules {

	private static final String[] STRIP_KEYS = {
		"probability",
		"prize_probability",
		"dailyStock",
		"daily_stock",
		"stock"
	};

	private TurntablePublicConfigRules() {}

	public static String displayStatus(long beginTime, long endTime, long nowEpochSec) {
		if (nowEpochSec < beginTime) {
			return "notstart";
		}
		if (nowEpochSec >= endTime) {
			return "expire";
		}
		return "online";
	}

	/** limit==0 → 不限，返回 null；否则 max(0, limit - used)。 */
	public static Long remainCount(long limit, long used) {
		if (limit == 0L) {
			return null;
		}
		long remain = limit - used;
		return remain < 0L ? 0L : remain;
	}

	public static boolean isUnlimited(long limit) {
		return limit == 0L;
	}

	public static List<Map<String, Object>> sanitizePrizesForPublic(List<Map<String, Object>> prizes) {
		List<Map<String, Object>> source = TurntablePrizeSelector.sortedCopy(prizes);
		List<Map<String, Object>> out = new ArrayList<>();
		if (source.isEmpty()) {
			return out;
		}
		for (int i = 0; i < source.size(); i++) {
			Map<String, Object> raw = source.get(i);
			Map<String, Object> n = new LinkedHashMap<>();
			String prizeId = firstText(raw.get("prize_id"));
			n.put("prize_id", prizeId);
			n.put("prizeId", prizeId);
			Object sort = raw.containsKey("sort") ? raw.get("sort") : (i + 1);
			n.put("sort", sort);
			n.put("sector_index", i);
			String name = firstText(raw.get("name"), raw.get("prize_title"));
			n.put("name", name);
			n.put("prize_title", name);
			String type = firstText(raw.get("type"), raw.get("prize_type")).toLowerCase();
			n.put("type", type);
			n.put("prize_type", type);
			Object value = raw.containsKey("value") ? raw.get("value") : raw.get("prize_value");
			n.put("value", value);
			n.put("prize_value", value);
			if (raw.containsKey("backgroundColor")) {
				n.put("backgroundColor", raw.get("backgroundColor"));
			}
			if (raw.containsKey("image")) {
				n.put("image", raw.get("image"));
			}
			for (String strip : STRIP_KEYS) {
				n.remove(strip);
			}
			out.add(n);
		}
		return out;
	}

	public static boolean containsSensitivePrizeFields(Map<String, Object> prize) {
		if (prize == null) {
			return false;
		}
		for (String k : STRIP_KEYS) {
			if (prize.containsKey(k)) {
				return true;
			}
		}
		return false;
	}

	private static String firstText(Object... values) {
		for (Object v : values) {
			if (v == null) {
				continue;
			}
			String t = Objects.toString(v, "").trim();
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		return "";
	}
}
