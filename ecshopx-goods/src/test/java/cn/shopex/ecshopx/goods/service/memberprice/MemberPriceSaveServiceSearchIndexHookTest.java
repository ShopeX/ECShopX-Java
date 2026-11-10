/**
 * Copyright 2019-2026 ShopeX
 */

package cn.shopex.ecshopx.goods.service.memberprice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsMemberPriceCheckRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsStartNumUpdateRepository;
import cn.shopex.ecshopx.goods.search.sync.GoodsSearchIndexDirtyFanout;
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
class MemberPriceSaveServiceSearchIndexHookTest {

	@Mock MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository;
	@Mock MemberPriceInsertRepository memberPriceInsertRepository;
	@Mock ItemsMemberPriceCheckRepository itemsMemberPriceCheckRepository;
	@Mock ItemsStartNumUpdateRepository itemsStartNumUpdateRepository;
	@Mock DistributorListQueryService distributorListQueryService;
	@Mock DistributorItemsRepository distributorItemsRepository;
	@Mock ItemsRepository itemsRepository;
	@Mock GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout;

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
						itemsRepository,
						goodsSearchIndexDirtyFanout);
	}

	@Test
	void saveMemberPrice_afterCommitFansOutDefaultItem() {
		Items owned = new Items();
		owned.setItemId(42L);
		owned.setDefaultItemId(99L);
		owned.setCompanyId(100L);
		when(itemsRepository.getByItemIdAndCompany(42L, 100L)).thenReturn(owned);
		when(itemsMemberPriceCheckRepository.existsHeadquartersPriceLessThanMember(
						eq(100L), eq(42L), org.mockito.ArgumentMatchers.anyLong()))
				.thenReturn(false);
		when(distributorListQueryService.listNonDeletedDistributorIdsForCompany(100L)).thenReturn(List.of());
		when(memberPriceInsertRepository.insert(any())).thenReturn(1);

		String mprice = "{\"42\":{\"grade\":{\"1\":\"1.00\"},\"vipGrade\":{\"1\":\"1.00\"}}}";

		service.saveMemberPrice(100L, "42", mprice);

		verify(goodsSearchIndexDirtyFanout).scheduleUpsertForDefaultItem(100L, 99L);
	}
}
