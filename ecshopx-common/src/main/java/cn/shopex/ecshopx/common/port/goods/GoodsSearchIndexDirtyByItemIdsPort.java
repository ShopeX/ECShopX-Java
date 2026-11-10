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

package cn.shopex.ecshopx.common.port.goods;

import java.util.Collection;

/** 营销写路径按 SKU item_id 触发商品搜索索引脏标记（解析 default_item_id 后扇出各店）。 */
public interface GoodsSearchIndexDirtyByItemIdsPort {

	void scheduleUpsertForItemIds(long companyId, Collection<Long> itemIds);
}
