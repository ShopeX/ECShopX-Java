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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsUpdatePatch;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsPriceStoreStatusWriteServiceSubmitAuditFilterTest {

	@Mock
	private SupplierItemsAdminBatchUpdateService supplierItemsAdminBatchUpdateService;

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	@Mock
	private ItemsTableLangSyncAfterMainUpdateService itemsTableLangSyncAfterMainUpdateService;

	@InjectMocks
	private ItemsPriceStoreStatusWriteService service;

	@Test
	void supplierSubmitProcessingIncludesLegacySubmitingSpelling() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("goods_id", List.of("337"));
		body.put("audit_status", "processing");

		service.updateFromRequest(38L, "supplier", body);

		ArgumentCaptor<SupplierItemsAdminBatchUpdateService.SupplierItemsFilter> filterCaptor =
				ArgumentCaptor.forClass(SupplierItemsAdminBatchUpdateService.SupplierItemsFilter.class);
		ArgumentCaptor<SupplierItemsUpdatePatch> patchCaptor =
				ArgumentCaptor.forClass(SupplierItemsUpdatePatch.class);
		verify(supplierItemsAdminBatchUpdateService)
				.batchUpdateItems(eq(38L), filterCaptor.capture(), patchCaptor.capture());

		List<String> auditIn = filterCaptor.getValue().getAuditStatusInOrNull();
		assertNotNull(auditIn);
		assertTrue(auditIn.contains("submiting"));
		assertTrue(auditIn.contains("submitting"));
		assertTrue(auditIn.contains("rejected"));
		assertTrue("processing".equals(patchCaptor.getValue().getAuditStatus()));
	}
}
