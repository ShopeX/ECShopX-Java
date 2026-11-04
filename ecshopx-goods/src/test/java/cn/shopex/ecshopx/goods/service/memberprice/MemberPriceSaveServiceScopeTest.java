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

package cn.shopex.ecshopx.goods.service.memberprice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsMemberPriceCheckRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsStartNumUpdateRepository;
import cn.shopex.ecshopx.promotions.repository.MemberPriceBulkDeleteRepository;
import cn.shopex.ecshopx.promotions.repository.MemberPriceInsertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberPriceSaveServiceScopeTest {

	@Mock MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository;
	@Mock MemberPriceInsertRepository memberPriceInsertRepository;
	@Mock ItemsMemberPriceCheckRepository itemsMemberPriceCheckRepository;
	@Mock ItemsStartNumUpdateRepository itemsStartNumUpdateRepository;
	@Mock DistributorListQueryService distributorListQueryService;
	@Mock DistributorItemsRepository distributorItemsRepository;
	@Mock ItemsRepository itemsRepository;

	MemberPriceSaveService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberPriceSaveService(
						new ObjectMapper(),
						memberPriceBulkDeleteRepository,
						memberPriceInsertRepository,
						itemsMemberPriceCheckRepository,
						itemsStartNumUpdateRepository,
						distributorListQueryService,
						distributorItemsRepository,
						itemsRepository);
	}

	@Test
	void save_foreignItemId_rejectsWithoutMemberPriceOrStartNumWrite() {
		when(itemsRepository.getByItemIdAndCompany(42L, 100L)).thenReturn(null);

		String mprice =
				"{\"42\":{\"grade\":{\"1\":\"1.00\"},\"vipGrade\":{\"1\":\"1.00\"},\"start_num\":3}}";

		assertThatThrownBy(() -> service.saveMemberPrice(100L, "42", mprice))
				.isInstanceOf(ResourceException.class)
				.hasMessage("商品不存在");

		verify(memberPriceBulkDeleteRepository, never()).deleteByCompanyIdAndItemIds(anyLong(), any());
		verify(memberPriceInsertRepository, never()).insert(any());
		verify(itemsStartNumUpdateRepository, never())
				.updateStartNumIfItemExists(anyLong(), anyLong(), anyInt());
	}

	@Test
	void save_ownCompanyItem_updatesStartNumWithCompanyScope() {
		Items owned = new Items();
		owned.setItemId(42L);
		owned.setCompanyId(100L);
		when(itemsRepository.getByItemIdAndCompany(42L, 100L)).thenReturn(owned);
		when(itemsMemberPriceCheckRepository.existsHeadquartersPriceLessThanMember(eq(100L), eq(42L), anyLong()))
				.thenReturn(false);
		when(distributorListQueryService.listNonDeletedDistributorIdsForCompany(100L)).thenReturn(List.of());
		when(memberPriceInsertRepository.insert(any())).thenReturn(1);

		String mprice =
				"{\"42\":{\"grade\":{\"1\":\"1.00\"},\"vipGrade\":{\"1\":\"1.00\"},\"start_num\":3}}";

		service.saveMemberPrice(100L, "42", mprice);

		verify(itemsStartNumUpdateRepository).updateStartNumIfItemExists(100L, 42L, 3);
	}
}
