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

package cn.shopex.ecshopx.merchant.service;

import org.springframework.util.StringUtils;

/** 只读判断 {@code main_category_selection} 是否非空（不依赖 goods 模块）。 */
public final class MerchantMainCategorySelectionJson {

	private MerchantMainCategorySelectionJson() {}

	public static boolean isNonEmptyStoredJson(String json) {
		if (!StringUtils.hasText(json)) {
			return false;
		}
		String t = json.trim();
		return !"[]".equals(t) && !"null".equalsIgnoreCase(t);
	}
}
