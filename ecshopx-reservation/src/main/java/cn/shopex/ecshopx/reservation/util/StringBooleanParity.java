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

package cn.shopex.ecshopx.reservation.util;

/**
 * 将常见查询字符串参数解释为布尔真值：{@code null}、空串与精确 {@code "0"} 为假，其余为真。
 */
public final class StringBooleanParity {

	private StringBooleanParity() {}

	/**
	 * @param raw 原始串；缺参对应 {@code null}
	 * @return 按上述字符串规则为真时返回 {@code true}
	 */
	public static boolean isTruthyBool(String raw) {
		if (raw == null || raw.isEmpty()) {
			return false;
		}
		return !"0".equals(raw);
	}
}
