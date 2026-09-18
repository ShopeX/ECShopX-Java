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

package cn.shopex.ecshopx.pointsmall.support;

/** 积分商品「单规格 / 多规格」判定（与删除、创建路径语义一致）。 */
public final class PointsmallItemsNospecSupport {

	private PointsmallItemsNospecSupport() {
	}

	/**
	 * {@code nospec} 为 false、0、"false"、"0"（宽松）时视为多规格；{@code null} 视为单规格。
	 */
	public static boolean isMultiSpec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		if (nospec instanceof Number n) {
			if (n.longValue() == 0L) {
				return true;
			}
		}
		String s = nospec.toString().trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}
}
