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

import java.util.Collection;
import java.util.Map;

/**
 * 拼团订单列表等场景下，按商品 ID 批量加载展示用字段（名称、价格、图片等）。
 */
public interface GroupOrderListItemFieldsPort {

	/**
	 * @param companyId 公司 ID
	 * @param itemIds   商品 ID 集合；重复与空 ID 由实现去重、忽略
	 * @return 键为 itemId；value 至少含 itemId、itemName、price（分）、pics
	 */
	Map<Long, Map<String, Object>> loadDisplayByItemIds(long companyId, Collection<Long> itemIds);
}
