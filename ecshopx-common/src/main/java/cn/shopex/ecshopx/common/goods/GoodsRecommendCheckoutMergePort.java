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

package cn.shopex.ecshopx.common.goods;

import java.util.Map;

/** 结算预览 / 下单前把推荐加购行 merge 进 {@code items}（由 goods 实现，orders 调用）。 */
public interface GoodsRecommendCheckoutMergePort {

	/**
	 * 若 {@code params} 含非空 {@code recommend_item_id}，校验后写入结算推荐行并去掉该字段。
	 * {@code getFreightFee} 与 {@code order_new} 同参调用。字段缺失或空数组为 no-op。
	 */
	void apply(long companyId, Map<String, Object> params);
}
