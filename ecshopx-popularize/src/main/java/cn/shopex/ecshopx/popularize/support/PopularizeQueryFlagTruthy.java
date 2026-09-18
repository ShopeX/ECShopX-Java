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

package cn.shopex.ecshopx.popularize.support;

/**
 * H5 query-string flag coercion for optional boolean-like flags in this module.
 *
 * <p>See analysis {@code docs/migration/artifacts/ecshopx-popularize/PromoterController/front-v1-indexCount-analysis.md}
 * and plan {@code docs/migration/artifacts/ecshopx-popularize/PromoterController/front-v1-indexCount-plan.md}
 * (sections on query truthiness and the salesman page flag).
 */
public final class PopularizeQueryFlagTruthy {

	private PopularizeQueryFlagTruthy() {}

	/**
	 * {@code HttpServletRequest#getParameter} value for H5 flags: only {@code null}, empty, or exactly {@code "0"}
	 * (ASCII, case-sensitive) are false; no {@code trim()} on {@code raw} for this decision.
	 */
	public static boolean isTruthyForH5QueryFlag(String raw) {
		if (raw == null) {
			return false;
		}
		if (raw.isEmpty()) {
			return false;
		}
		if ("0".equals(raw)) {
			return false;
		}
		return true;
	}
}
