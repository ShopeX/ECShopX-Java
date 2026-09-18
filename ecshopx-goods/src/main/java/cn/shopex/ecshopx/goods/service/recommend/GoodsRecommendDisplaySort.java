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

package cn.shopex.ecshopx.goods.service.recommend;

/** 商品推荐展示排序枚举（SSOT §3.4） */
public final class GoodsRecommendDisplaySort {

	public static final String SALES_DESC = "sales_desc";

	public static final String PRICE_ASC = "price_asc";

	private GoodsRecommendDisplaySort() {}

	public static boolean isValid(String sort) {
		return SALES_DESC.equals(sort) || PRICE_ASC.equals(sort);
	}
}
