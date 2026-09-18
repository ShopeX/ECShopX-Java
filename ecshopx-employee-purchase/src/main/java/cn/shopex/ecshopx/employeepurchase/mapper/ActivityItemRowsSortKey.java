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

package cn.shopex.ecshopx.employeepurchase.mapper;

/**
 * Whitelisted ORDER BY variants for activity item list rows; never derived from raw request strings.
 */
public enum ActivityItemRowsSortKey {

	SALES,
	ACTIVITY_PRICE_DESC,
	ACTIVITY_PRICE_ASC,
	/** Default goods sort: `sort` column then `item_id`. */
	SORT_DESC,
	/** Rows ordered by `item_id` only (back-office list). */
	ITEM_ID_DESC
}
