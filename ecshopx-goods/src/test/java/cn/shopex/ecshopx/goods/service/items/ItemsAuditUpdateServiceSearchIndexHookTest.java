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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.search.sync.GoodsSearchIndexDirtyFanout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsAuditUpdateServiceSearchIndexHookTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout;

	@InjectMocks
	private ItemsAuditUpdateService service;

	@Test
	void auditItems_approved_fansOutSearchIndexForDefaultItem() {
		Items sku = new Items();
		sku.setItemId(42L);
		sku.setGoodsId(100L);
		sku.setDefaultItemId(99L);
		sku.setCompanyId(38L);
		when(itemsRepository.listByCompanyIdAndGoodsId(38L, 100L, false)).thenReturn(List.of(sku));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("goods_id", 100L);
		body.put("audit_status", "approved");
		body.put("audit_reason", "");

		service.auditItems(38L, body);

		verify(itemsRepository)
				.updateAuditFieldsByCompanyAndGoodsId(
						eq(38L), any(ItemsRepository.GoodsIdClause.class), eq("approved"), eq(""));
		verify(goodsSearchIndexDirtyFanout).scheduleUpsertForDefaultItem(38L, 99L);
	}
}
