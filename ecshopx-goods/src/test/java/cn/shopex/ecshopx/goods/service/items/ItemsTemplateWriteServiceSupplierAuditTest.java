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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsTemplateWriteServiceSupplierAuditTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private SupplierItemsRepository supplierItemsRepository;

	@InjectMocks
	private ItemsTemplateWriteService service;

	@Test
	void supplierTemplateChangeMarksAuditProcessing() {
		SupplierItems row = new SupplierItems();
		row.setItemId(9001L);
		row.setCompanyId(38L);
		when(supplierItemsRepository.listByDefaultItemIdAndCompany(337L, 38L)).thenReturn(List.of(row));
		when(itemsRepository.countBySupplierItemIds(List.of(9001L))).thenReturn(0L);

		service.setItemsTemplate(38L, true, 55, List.of(337L));

		verify(supplierItemsRepository).updateTemplatesIdByDefaultItemIdAndCompany(337L, 38L, 55);
		verify(supplierItemsRepository)
				.updateAuditStatusByDefaultItemIdAndCompany(eq(337L), eq(38L), eq("processing"));
	}
}
