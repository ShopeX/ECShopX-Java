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

package cn.shopex.ecshopx.common.inventory;

/** 订单行库存扣减/返还目标池（互斥，每行仅一个）。 */
public enum InventoryDeductTarget {
	SUPPLIER_ITEMS,
	PLATFORM_ITEMS,
	/** 店铺独立库存：Redis 带请求店前缀，回写 distributor_items.store */
	SHOP_DISTRIBUTOR_ITEMS
}
