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

package cn.shopex.ecshopx.common.promotions;

import java.util.List;
import java.util.Map;

/** 运营端「猜你喜欢」完整商品列表（平台主表查询 + 加工链），供 promotions 模块调用。 */
public interface RecommendLikeAdminItemsFullListPort {

	/**
	 * @param companyId 企业 ID
	 * @param itemIds 非 null 且非空；空列表由调用方短路，禁止调用本方法
	 * @param acceptLanguageHeader 可为 null
	 * @return 仅含 {@code total_count}（Long）、{@code list}（List）
	 */
	Map<String, Object> queryFullItemsList(long companyId, List<Long> itemIds, String acceptLanguageHeader);
}
