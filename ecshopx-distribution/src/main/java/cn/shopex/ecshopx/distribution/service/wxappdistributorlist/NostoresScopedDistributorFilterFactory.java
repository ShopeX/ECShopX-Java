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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist;

import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListFilter;
import cn.shopex.ecshopx.orders.service.nostores.dto.NostoresScopedDistributorFilter;

public final class NostoresScopedDistributorFilterFactory {

	private NostoresScopedDistributorFilterFactory() {}

	public static NostoresScopedDistributorFilter fromWxappShopListFilter(DistributorWxappShopListFilter f) {
		return new NostoresScopedDistributorFilter(
				f.getCompanyId(),
				f.getDistributorIdInList(),
				f.getIsZiti(),
				f.getIsDelivery(),
				f.getIsDada(),
				f.isRequireShopIdNonEmpty(),
				f.getProvinceLikeEscaped(),
				f.getCityLikeEscaped(),
				f.getAreaLikeEscaped(),
				f.getSearchType(),
				f.getNameLikeEscaped(),
				f.getOrDistributorIdsFromItems(),
				true);
	}
}
