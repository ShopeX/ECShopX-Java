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

package cn.shopex.ecshopx.kaquan.service.discount.dto;

import java.util.List;
import lombok.Data;

/**
 * MyBatis 入参：用户券列表分页与按商品筛 id 分支共用（XML {@code parameterType} 与此类一致）。
 */
@Data
public class UserDiscountListFilterParams {

	private Long companyId;
	private Long userId;
	private List<String> cardTypes;
	private String usePlatform;
	private String useScenes;
	private List<Integer> statuses;
	private Integer nowEpoch;
	private Boolean validOnly;
	private String code;
	private Long cardId;
	private Integer leastCostLte;
	private Long distributorId;
	/** platform / standard / b2c 等，与店铺范围 SQL 分支一致 */
	private String productModel;
	/** 非空时追加 {@code kudc.id IN (...)}（购物车商品维度的用户券 id） */
	private List<Long> filterUserDiscountIds;
	/** {@link DiscountCardUserCardIdsByGoodsService} 各支路：与 {@code rel_item_ids} LIKE 组合的商品/类目/标签/品牌 id */
	private List<Long> goodsItemIds;
}
