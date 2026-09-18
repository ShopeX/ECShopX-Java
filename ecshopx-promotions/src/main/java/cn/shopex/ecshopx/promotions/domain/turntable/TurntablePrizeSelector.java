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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.util.StringUtils;

/**
 * PRD §5.4：随机数 1～100（闭区间），按 sort 升序累计 probability；未命中落到排序最前的 thanks。
 */
public final class TurntablePrizeSelector {

	public static final int RANDOM_MIN = 1;
	public static final int RANDOM_MAX = 100;

	private TurntablePrizeSelector() {}

	public record DrawPick(int randomValue, int sectorIndex, Map<String, Object> prize, boolean remainderHit) {}

	public static List<Map<String, Object>> sortedCopy(List<Map<String, Object>> prizes) {
		List<Map<String, Object>> copy = new ArrayList<>();
		if (prizes == null) {
			return copy;
		}
		List<int[]> order = new ArrayList<>();
		for (int i = 0; i < prizes.size(); i++) {
			Map<String, Object> p = prizes.get(i);
			if (p == null) {
				continue;
			}
			order.add(new int[] {intOr(p.get("sort"), Integer.MAX_VALUE), i});
		}
		order.sort(Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt(a -> a[1]));
		for (int[] pair : order) {
			copy.add(prizes.get(pair[1]));
		}
		return copy;
	}

	public static DrawPick pick(List<Map<String, Object>> prizes) {
		return pick(prizes, ThreadLocalRandom.current().nextInt(RANDOM_MIN, RANDOM_MAX + 1));
	}

	public static DrawPick pick(List<Map<String, Object>> prizes, int randomValue) {
		List<Map<String, Object>> sorted = sortedCopy(prizes);
		if (sorted.isEmpty()) {
			throw new IllegalArgumentException("prizes empty");
		}
		int rand = randomValue;
		if (rand < RANDOM_MIN || rand > RANDOM_MAX) {
			rand = RANDOM_MIN;
		}
		int acc = 0;
		for (int i = 0; i < sorted.size(); i++) {
			int p = probabilityOf(sorted.get(i));
			if (p <= 0) {
				continue;
			}
			acc += p;
			if (rand <= acc) {
				return new DrawPick(rand, i, sorted.get(i), false);
			}
		}
		int thanksIdx = indexOfFirstThanks(sorted);
		return new DrawPick(rand, thanksIdx, sorted.get(thanksIdx), true);
	}

	public static int indexOfFirstThanks(List<Map<String, Object>> prizes) {
		for (int i = 0; i < prizes.size(); i++) {
			if (TurntablePrizeType.THANKS.equalsIgnoreCase(typeOf(prizes.get(i)))) {
				return i;
			}
		}
		return -1;
	}

	public static String typeOf(Map<String, Object> prize) {
		if (prize == null) {
			return "";
		}
		Object t = prize.containsKey("type") ? prize.get("type") : prize.get("prize_type");
		return t == null ? "" : String.valueOf(t).trim();
	}

	public static int probabilityOf(Map<String, Object> prize) {
		if (prize == null) {
			return 0;
		}
		Object raw = prize.containsKey("probability") ? prize.get("probability") : prize.get("prize_probability");
		return intOr(raw, 0);
	}

	public static Map<String, Object> findByPrizeId(List<Map<String, Object>> prizes, String prizeId) {
		if (!StringUtils.hasText(prizeId) || prizes == null) {
			return null;
		}
		for (Map<String, Object> p : prizes) {
			if (prizeId.equals(TurntableDrawResponseMapper.prizeIdOf(p))) {
				return p;
			}
		}
		return null;
	}

	private static int intOr(Object raw, int fallback) {
		if (raw == null) {
			return fallback;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
