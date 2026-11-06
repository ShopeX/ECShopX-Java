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

package cn.shopex.ecshopx.common.goods.port;

import java.util.Map;

/**
 * 入驻/商户主类目校验与详情补全，由 {@code ecshopx-goods} 实现，避免 merchant↔goods Maven 环。
 */
public interface MerchantSettlementMainCategoryPort {

	/**
	 * 校验 {@code main_category_selection} 并返回落库 JSON（仅含 category_id、level，已去重）。
	 */
	String validateNormalizeAndSerialize(long companyId, Object rawSelection);

	/**
	 * 向详情 row 写入 {@code main_category_selection}（含 name 等）、{@code main_category_names}、
	 * {@code main_category_resolved_level3_ids}。
	 */
	void enrichDetailCategoryFields(long companyId, String storedSelectionJson, Map<String, Object> targetRow);
}
