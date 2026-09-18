package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchJobNames;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsPagedAddRunner;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsPagedAddRunner.AddJobParams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorGoodsSyncServiceAddDistributorItemsDispatchTest {

	private DistributorMapper distributorMapper;
	private DistributorItemsPagedAddRunner pagedAddRunner;
	private DistributorGoodsSyncService sut;

	private static final long COMPANY_ID = 7001L;
	private static final long DEFAULT_ITEM_ID = 88_001L;

	@BeforeEach
	void setUp() {
		distributorMapper = mock(DistributorMapper.class);
		pagedAddRunner = mock(DistributorItemsPagedAddRunner.class);
		sut = new DistributorGoodsSyncService(distributorMapper, pagedAddRunner);
	}

	/**
	 * Platform create-items path fans out via {@link DistributionDispatchJobNames#ADD_DISTRIBUTOR_ITEMS_JOB}
	 * (must include {@code job:62}).
	 */
	@Test
	void platformCreateItemsPath_addDistributorItemsJobNameContainsJob62() {
		assertTrue(DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB.contains("job:62"));
	}

	@Test
	void syncGoods_whenMoreThanThreeAutoSyncDistributors_enqueuesAsyncFirstPagePerDistributor() {
		when(distributorMapper.selectList(any())).thenReturn(distributors(4, COMPANY_ID));
		sut.syncGoods(COMPANY_ID, DEFAULT_ITEM_ID);
		ArgumentCaptor<AddJobParams> captor = ArgumentCaptor.forClass(AddJobParams.class);
		verify(pagedAddRunner, times(4)).enqueueAsyncFirstPage(captor.capture());
		verify(pagedAddRunner, never()).runAllPagesSync(any());
		assertJobParamsBatch(captor.getAllValues());
	}

	@Test
	void syncGoods_whenAtMostThreeAutoSyncDistributors_runsAllPagesSyncPerDistributor() {
		when(distributorMapper.selectList(any())).thenReturn(distributors(3, COMPANY_ID));
		sut.syncGoods(COMPANY_ID, DEFAULT_ITEM_ID);
		ArgumentCaptor<AddJobParams> captor = ArgumentCaptor.forClass(AddJobParams.class);
		verify(pagedAddRunner, times(3)).runAllPagesSync(captor.capture());
		verify(pagedAddRunner, never()).enqueueAsyncFirstPage(any());
		assertJobParamsBatch(captor.getAllValues());
	}

	@Test
	void syncGoods_whenNoAutoSyncDistributors_doesNotTouchRunner() {
		when(distributorMapper.selectList(any())).thenReturn(Collections.emptyList());
		sut.syncGoods(COMPANY_ID, DEFAULT_ITEM_ID);
		verifyNoInteractions(pagedAddRunner);
	}

	private static void assertJobParamsBatch(List<AddJobParams> captured) {
		for (int i = 0; i < captured.size(); i++) {
			AddJobParams p = captured.get(i);
			assertEquals(COMPANY_ID, p.companyId());
			assertEquals(1000L + i, p.distributorId());
			assertEquals(List.of(DEFAULT_ITEM_ID), p.defaultItemIdInFilter());
			assertFalse(p.isCanSale());
			assertEquals(1, p.page());
			assertEquals(100, p.pageSize());
		}
	}

	private static List<Distributor> distributors(int count, long companyId) {
		List<Distributor> out = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Distributor d = new Distributor();
			d.setDistributorId(1000L + i);
			d.setCompanyId(companyId);
			out.add(d);
		}
		return out;
	}
}
