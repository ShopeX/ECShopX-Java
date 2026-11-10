package cn.shopex.ecshopx.goods.search.sync;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsSearchIndexDirtyFanoutTest {

	private static final long COMPANY_ID = 7L;
	private static final long DEFAULT_ITEM_ID = 501L;

	@Mock
	private DistributorItemsRepository distributorItemsRepository;

	@Mock
	private SearchIndexSyncScheduler searchIndexSyncScheduler;

	private GoodsSearchIndexDirtyFanout sut;

	@BeforeEach
	void setUp() {
		sut = new GoodsSearchIndexDirtyFanout(distributorItemsRepository, searchIndexSyncScheduler);
	}

	@Test
	void scheduleUpsertForDefaultItem_fansOutHeadquartersAndShops() {
		when(distributorItemsRepository.listDistinctDistributorIdsByCompanyAndDefaultItemId(COMPANY_ID, DEFAULT_ITEM_ID))
				.thenReturn(List.of(11L, 22L));

		sut.scheduleUpsertForDefaultItem(COMPANY_ID, DEFAULT_ITEM_ID);

		verify(searchIndexSyncScheduler).scheduleUpsert(COMPANY_ID, 0L, DEFAULT_ITEM_ID);
		verify(searchIndexSyncScheduler).scheduleUpsert(COMPANY_ID, 11L, DEFAULT_ITEM_ID);
		verify(searchIndexSyncScheduler).scheduleUpsert(COMPANY_ID, 22L, DEFAULT_ITEM_ID);
	}

	@Test
	void scheduleDeleteForDefaultItem_fansOutHeadquartersAndShops() {
		when(distributorItemsRepository.listDistinctDistributorIdsByCompanyAndDefaultItemId(COMPANY_ID, DEFAULT_ITEM_ID))
				.thenReturn(List.of(9L));

		sut.scheduleDeleteForDefaultItem(COMPANY_ID, DEFAULT_ITEM_ID);

		verify(searchIndexSyncScheduler).scheduleDelete(COMPANY_ID, 0L, DEFAULT_ITEM_ID);
		verify(searchIndexSyncScheduler).scheduleDelete(COMPANY_ID, 9L, DEFAULT_ITEM_ID);
	}

	@Test
	void scheduleUpsertForDefaultItem_skipsWhenDefaultItemIdInvalid() {
		sut.scheduleUpsertForDefaultItem(COMPANY_ID, 0L);
		verify(searchIndexSyncScheduler, never()).scheduleUpsert(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
	}
}
