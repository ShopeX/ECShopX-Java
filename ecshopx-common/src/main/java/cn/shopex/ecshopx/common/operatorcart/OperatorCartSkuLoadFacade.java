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

package cn.shopex.ecshopx.common.operatorcart;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 运营购物车读取 SKU、店铺价替换、会员价、标签等（由商品模块实现，companys / kaquan 仅依赖本接口）。
 */
public interface OperatorCartSkuLoadFacade {

	List<OperatorCartSkuRowDto> loadSkus(long companyId, long distributorId, long targetUserId, List<Long> itemIds);

	List<OperatorCartSkuRowDto> applyDistributorSkuReplace(long companyId, long distributorId, String productModel,
			List<OperatorCartSkuRowDto> rows);

	void applyMemberPrices(long companyId, long targetUserId, List<OperatorCartSkuRowDto> rows);

	List<Long> listTagIdsByItemIds(long companyId, Collection<Long> defaultItemIds);

	/**
	 * 按购物车 SKU 加载券门槛匹配所需的类目、品牌、标签（标签关联键为 SKU 或 SPU {@code default_item_id}）。
	 */
	Map<Long, CouponCartItemScope> loadCouponCartItemScopes(long companyId, Collection<Long> cartItemIds);

	/**
	 * 按购物车 SKU 批量补全主商品、主类目、品牌等，写入 {@code filter} 的 {@code default_item_id}、{@code item_main_cat_id}、{@code brand_id} 列表键（与卡券筛选约定一致）。
	 */
	void fillCouponGoodsFilterLists(long companyId, List<Long> cartItemIds, Map<String, Object> filter);

	default boolean hasCompleteGoodsScopeKeys(Map<String, Object> filter) {
		return filter != null && filter.containsKey("item_main_cat_id") && filter.containsKey("brand_id")
				&& filter.containsKey("default_item_id");
	}

	/**
	 * 运营端加购单 SKU 解析：平台/店铺分支、分销行合并库存价，与运营购物车加购校验一致。
	 */
	OperatorCartSkuRowDto resolveSkuForOperatorCartAdd(long companyId, long distributorId, long itemId, String productModel);
}
