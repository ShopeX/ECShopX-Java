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

package cn.shopex.ecshopx.orders.support;

import java.util.Objects;

public final class ScalarEmptyCompat {

	private ScalarEmptyCompat() {}

	public static boolean isEmpty(Object o) {
		if (o == null) {
			return true;
		}
		if (Boolean.FALSE.equals(o)) {
			return true;
		}
		if (Boolean.TRUE.equals(o)) {
			return false;
		}
		if (o instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		String t = Objects.toString(o).trim();
		return t.isEmpty();
	}

	public static boolean isNotEmpty(Object o) {
		return !isEmpty(o);
	}
}
