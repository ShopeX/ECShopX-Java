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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Replaces {@code kaquan_rel_items} rows for a discount card's {@code item_type = normal} scope,
 * matching the legacy card maintenance flow used when tag-to-item mappings change.
 */
@Service
public class DiscountCardNormalRelItemsReplacementService {

	private static final int INSERT_CHUNK = 100;

	private final RelItemsMapper relItemsMapper;

	public DiscountCardNormalRelItemsReplacementService(RelItemsMapper relItemsMapper) {
		this.relItemsMapper = relItemsMapper;
	}

	public void replace(long companyId, long cardId, List<Long> itemIds) {
		relItemsMapper.delete(new LambdaQueryWrapper<RelItems>()
				.eq(RelItems::getCompanyId, companyId)
				.eq(RelItems::getCardId, cardId)
				.eq(RelItems::getItemType, "normal"));
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		List<KaquanRelItemBatchRow> batch = new ArrayList<>();
		for (Long itemId : itemIds) {
			if (itemId == null || itemId <= 0L) {
				continue;
			}
			KaquanRelItemBatchRow row = new KaquanRelItemBatchRow();
			row.setItemId(itemId);
			row.setCardId(cardId);
			row.setCompanyId(companyId);
			row.setItemType("normal");
			row.setIsShow(1);
			row.setUseLimit(0);
			batch.add(row);
			if (batch.size() >= INSERT_CHUNK) {
				relItemsMapper.insertBatch(batch);
				batch.clear();
			}
		}
		if (!batch.isEmpty()) {
			relItemsMapper.insertBatch(batch);
		}
	}
}
