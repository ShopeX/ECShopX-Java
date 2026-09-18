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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.port.goods.ItemBatchEditStatusMarketingSkuRowsPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ItemBatchEditStatusMarketingSkuRowsAdapter implements ItemBatchEditStatusMarketingSkuRowsPort {

	private final ItemsRepository itemsRepository;

	public ItemBatchEditStatusMarketingSkuRowsAdapter(ItemsRepository itemsRepository) {
		this.itemsRepository = itemsRepository;
	}

	@Override
	public List<Map<String, Object>> listRows(long companyId, long goodsId) {
		List<Items> rows = itemsRepository.listByCompanyIdAndGoodsId(companyId, goodsId, false);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (Items it : rows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("item_bn", it.getItemBn() == null ? "" : it.getItemBn());
			row.put("approve_status", it.getApproveStatus() == null ? "" : it.getApproveStatus());
			out.add(row);
		}
		return out;
	}
}
