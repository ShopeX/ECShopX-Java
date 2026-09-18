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

package cn.shopex.ecshopx.common.espier.upload;

import java.util.Collection;
import java.util.Map;

/**
 * Batch item lookup for marketing / purchase / discount syncProcess Excel uploads.
 *
 * <p>Returned map is keyed by trimmed {@code item_bn}. Each value carries the DB fields needed to
 * enrich an Excel row ({@code item_id}, {@code default_item_id}, {@code pics}, {@code market_price},
 * {@code item_name}, {@code item_type}, {@code store}, {@code price}).
 */
public interface EspierMarketingStyleItemsLookupPort {

	/**
	 * MarketingGoods path: apply {@code distributor_id} (and {@code product_model} when {@code
	 * distributorId > 0}).
	 */
	Map<String, Map<String, Object>> lookupForMarketing(
			long companyId, long distributorId, Collection<String> itemBns);

	/**
	 * PurchaseGoods / DiscountGoods path: resolve by {@code item_bn} within the company only (no
	 * distributor filter).
	 */
	Map<String, Map<String, Object>> lookupByItemBn(long companyId, Collection<String> itemBns);
}
