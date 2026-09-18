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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DistributorItemsSkuPageQueryService {

	private final ItemsListQueryRepository itemsListQueryRepository;

	public DistributorItemsSkuPageQueryService(ItemsListQueryRepository itemsListQueryRepository) {
		this.itemsListQueryRepository = itemsListQueryRepository;
	}

	public long countSkus(long companyId, List<Long> defaultItemIdInOrNull) {
		if (defaultItemIdInOrNull == null) {
			return itemsListQueryRepository.countSkusNormalByCompanyAndDefaultItemIds(companyId, null);
		}
		return itemsListQueryRepository.countSkusNormalByCompanyAndDefaultItemIds(companyId, defaultItemIdInOrNull);
	}

	public List<Items> selectPage(long companyId, List<Long> defaultItemIdInOrNull, int page, int pageSize) {
		int ps = Math.min(Math.max(pageSize, 1), 2000);
		int p = Math.max(page, 1);
		int offset = (p - 1) * ps;
		if (defaultItemIdInOrNull == null) {
			return itemsListQueryRepository.selectSkusNormalPageAsc(companyId, null, offset, ps);
		}
		return itemsListQueryRepository.selectSkusNormalPageAsc(companyId, defaultItemIdInOrNull, offset, ps);
	}
}
