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

package cn.shopex.ecshopx.goods.service.items;

final class ItemDeleteSpec {

	private ItemDeleteSpec() {
	}

	static boolean isMultiSpec(String nospec) {
		if (nospec == null) {
			return false;
		}
		String t = nospec.trim().toLowerCase();
		return "false".equals(t) || "0".equals(t);
	}

	/**
	 * Supplier-side delete: multi-spec when {@code nospec} is absent, blank, or explicitly false/0.
	 */
	static boolean isMultiSpecForSupplier(String nospec) {
		if (nospec == null) {
			return true;
		}
		String t = nospec.trim();
		if (t.isEmpty()) {
			return true;
		}
		String lower = t.toLowerCase();
		return "false".equals(lower) || "0".equals(lower);
	}
}
