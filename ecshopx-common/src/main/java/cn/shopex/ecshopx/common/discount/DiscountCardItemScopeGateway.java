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

package cn.shopex.ecshopx.common.discount;

import java.util.Collection;
import java.util.List;

/**
 * 标准优惠券创建时，类目/标签/品牌范围与商品计数的只读门面（由商品模块实现，供卡券模块注入）。
 */
public interface DiscountCardItemScopeGateway {

	/**
	 * 主类目名称，按逗号拼接，用于适用范围展示或持久化。
	 */
	String joinCategoryNames(long companyId, Collection<Long> categoryIds);

	/**
	 * 商品标签名称，逗号拼接。
	 */
	String joinTagNames(long companyId, List<Long> tagIds);

	/**
	 * 品牌属性名称，逗号拼接（attribute_type = brand）。
	 */
	String joinBrandNames(long companyId, List<Integer> brandIds);

	/**
	 * 指定企业与标签集合下，商品与标签关联表中的行数。
	 */
	long countTagRelations(long companyId, List<Long> tagIds);

	/**
	 * 在给定类目、标签、品牌（及可选店铺范围）条件下，符合适用范围规则的商品总数。
	 *
	 * @param distributorIdFirstOrNull 若 isDistributorScope 为 true，则取店铺维度第一条 distributor_id；否则为 null
	 */
	long countApplicableItems(long companyId, Integer distributorIdFirstOrNull, boolean isDistributorScope,
			List<Long> categoryIds, List<Long> tagIds, List<Integer> brandIds);
}
