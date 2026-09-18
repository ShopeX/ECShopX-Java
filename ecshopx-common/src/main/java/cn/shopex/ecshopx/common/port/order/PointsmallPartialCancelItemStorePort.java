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

package cn.shopex.ecshopx.common.port.order;

/**
 * 积分商城订单明细库存调整（Redis {@code pointsmall_item_store:*} 与总部商品库存列同步）。
 */
public interface PointsmallPartialCancelItemStorePort {

	/**
	 * 按数量符号调整 Redis 与总部库存：{@code num} 为负表示回补库存，为正表示扣减；返回是否成功（回补导致库存为负等视为失败）。
	 */
	boolean minusItemStore(long companyId, long itemId, int num, boolean isTotalStore);
}
