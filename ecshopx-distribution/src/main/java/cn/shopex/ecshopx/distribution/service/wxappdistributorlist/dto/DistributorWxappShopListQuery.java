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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto;

import java.util.List;

public record DistributorWxappShopListQuery(
		int page,
		int pageSize,
		String lng,
		String lat,
		String province,
		String city,
		String area,
		String address,
		int type,
		int showTag,
		int showDiscount,
		int showMarketingActivity,
		int showSalesCount,
		int showScore,
		int showItems,
		String itemTagIdRaw,
		int searchType,
		String name,
		String distributorIdRaw,
		List<String> distributorIds,
		Integer isZiti,
		Integer isDelivery,
		Integer isDada,
		String distributorCategoryIdRaw,
		String excludeDistributorIdRaw,
		String isValidRaw,
		String getShopRaw,
		String cardId,
		String isNostoresRaw,
		String cartType,
		String orderType,
		String seckillId,
		String seckillTicket,
		String iscrossborder,
		String bargainId,
		String distributorTagIdRaw,
		int sortType,
		String showType) {
}
