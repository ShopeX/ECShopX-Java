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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将退款金额（分）按行权重比例拆分，最后一行吃余数。对齐 PHP {@code ShuyunOpenPlatformRefundLineFeeAllocator}。
 */
public final class RefundLineFeeAllocator {

	private RefundLineFeeAllocator() {}

	public static Map<Object, Integer> allocateProportional(int totalRefundFen, Map<?, Integer> weights) {
		List<Object> keys = new ArrayList<>(weights.keySet());
		Map<Object, Integer> out = new LinkedHashMap<>();
		if (keys.isEmpty()) {
			return out;
		}
		if (totalRefundFen <= 0) {
			for (Object k : keys) {
				out.put(k, 0);
			}
			return out;
		}
		int sumWeight = 0;
		for (Integer w : weights.values()) {
			sumWeight += Math.max(0, w == null ? 0 : w);
		}
		if (sumWeight <= 0) {
			for (Object k : keys) {
				out.put(k, 0);
			}
			out.put(keys.get(0), totalRefundFen);
			return out;
		}
		int remaining = totalRefundFen;
		int n = keys.size();
		for (int i = 0; i < n; i++) {
			Object k = keys.get(i);
			if (i == n - 1) {
				out.put(k, Math.max(0, remaining));
				break;
			}
			int w = Math.max(0, weights.get(k) == null ? 0 : weights.get(k));
			int part = (int) Math.floor((double) totalRefundFen * w / sumWeight);
			out.put(k, part);
			remaining -= part;
		}
		return out;
	}
}
