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

package cn.shopex.ecshopx.companys.service.setting;

/**
 * Loose equality to the literal four-character string {@code "false"}, aligned with legacy numeric/string coercion
 * rules configurable per deployment.
 */
public final class FalseStringLooseEqualityHelper {

	private FalseStringLooseEqualityHelper() {}

	public enum Mode {
		/** Numeric zero compares equal to the literal {@code "false"} string under legacy rules. */
		LEGACY,
		/** Finite numeric values never compare equal to the literal {@code "false"} string. */
		MODERN;
	}

	public static Mode fromProperty(String raw) {
		if (raw == null || raw.isBlank()) {
			return Mode.MODERN;
		}
		String s = raw.trim();
		if ("MODERN".equalsIgnoreCase(s)) {
			return Mode.MODERN;
		}
		if ("LEGACY".equalsIgnoreCase(s)) {
			return Mode.LEGACY;
		}
		throw new IllegalArgumentException(
				"Invalid ecshopx.companys.item-start-num.weak-string-equals-mode: "
						+ raw
						+ " (allowed: MODERN, LEGACY)");
	}

	/**
	 * Whether {@code v} is loosely equal to the literal {@code "false"} string under {@code mode}. The persisted
	 * boolean flag is typically {@code !looselyEqualsFalseString(v, mode)}.
	 */
	public static boolean looselyEqualsFalseString(Object v, Mode mode) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return false;
			}
			if (mode == Mode.LEGACY) {
				return d == 0.0d;
			}
			return false;
		}
		if (v instanceof CharSequence seq) {
			return "false".contentEquals(seq);
		}
		return false;
	}
}
