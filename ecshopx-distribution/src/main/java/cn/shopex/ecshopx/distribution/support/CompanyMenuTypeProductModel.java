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

package cn.shopex.ecshopx.distribution.support;

/**
 * Maps company {@code menu_type} codes to product-model slugs (e.g. after-sales and geocoding behaviour).
 */
public final class CompanyMenuTypeProductModel {

	private CompanyMenuTypeProductModel() {
	}

	public static String toSlug(Integer menuType) {
		if (menuType == null) {
			return "platform";
		}
		return switch (menuType) {
			case 1 -> "all";
			case 2 -> "b2c";
			case 3 -> "platform";
			case 4 -> "standard";
			case 5 -> "in_purchase";
			default -> "platform";
		};
	}
}
