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

package cn.shopex.ecshopx.goods.service.export;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsTagsQueryService;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeFilterBuildResult;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsListQueryRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportItemsCodeFilterBuildServiceTest {

	private static final long COMPANY_ID = 38L;

	@Mock private ItemsListQueryRepository itemsListQueryRepository;
	@Mock private SupplierItemsListQueryRepository supplierItemsListQueryRepository;
	@Mock private SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	@Mock private ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	@Mock private ItemsTagsQueryService itemsTagsQueryService;
	@Mock private DistributorListQueryService distributorListQueryService;
	@Mock private DistributorItemsRepository distributorItemsRepository;
	@Mock private ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	private ExportItemsCodeFilterBuildService service;

	@BeforeEach
	void setUp() {
		service = new ExportItemsCodeFilterBuildService(
				itemsListQueryRepository,
				supplierItemsListQueryRepository,
				supplierOperatorQueryRepository,
				itemsCategoryItemIdResolver,
				itemsTagsQueryService,
				distributorListQueryService,
				distributorItemsRepository,
				itemsCategoryDistributorIdResolver);
	}

	@Test
	@DisplayName("standard 店铺账号 body distributor_id=0 时用 JWT 店 ID 展开全量导出")
	void build_distributorStandard_unselectedExport_usesJwtShopId() {
		when(itemsCategoryDistributorIdResolver.resolveProductModel(COMPANY_ID)).thenReturn("standard");
		when(distributorItemsRepository.existsAnyForCompanyAndDistributorIds(eq(COMPANY_ID), eq(List.of(129L))))
				.thenReturn(true);
		when(itemsListQueryRepository.listDistinctDefaultItemIdsByFullParams(any())).thenReturn(List.of(1001L));

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("distributor_id", 0);
		input.put("item_type", "normal");
		Map<String, Object> jwt = Map.of("distributor_id", 129L, "operator_type", "distributor");

		ExportItemsCodeFilterBuildResult built =
				service.build(COMPANY_ID, 1L, "distributor", null, input, jwt);

		ExportItemsCodeFilterBuildResult.ParamsReady ready = assertInstanceOf(ExportItemsCodeFilterBuildResult.ParamsReady.class, built);
		Object ids = ready.params().get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		assertTrue(ids instanceof List<?> list && list.contains(1001L));
	}
}
