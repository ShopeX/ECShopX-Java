package cn.shopex.ecshopx.promotions.service.salesperson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemsShelves;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonItemsShelvesMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SeckillActivityItemsShelvesSyncContributorTest {

	private static final long COMPANY_ID = 100L;
	private static final long ACTIVITY_ID = 200L;

	@Test
	void sync_whenUseBoundAllShops_ignoresDistributorCsv_andInsertsOnlyZero() {
		SeckillActivityMapper activityMapper = Mockito.mock(SeckillActivityMapper.class);
		SeckillRelGoodsMapper relMapper = Mockito.mock(SeckillRelGoodsMapper.class);
		SalespersonItemsShelvesMapper shelvesMapper = Mockito.mock(SalespersonItemsShelvesMapper.class);

		SeckillActivity activity = baselineActivity();
		activity.setUseBound(0);
		activity.setDistributorId("7,8");

		when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(activity);
		when(relMapper.selectList(any())).thenReturn(List.of(relRow(300L)));

		var contributor = new SeckillActivityItemsShelvesSyncContributor(activityMapper, relMapper, shelvesMapper);
		contributor.sync(COMPANY_ID, ACTIVITY_ID);

		ArgumentCaptor<SalespersonItemsShelves> cap = ArgumentCaptor.forClass(SalespersonItemsShelves.class);
		verify(shelvesMapper, times(1)).insert(cap.capture());
		assertEquals(0L, cap.getValue().getDistributorId().longValue());
	}

	@Test
	void sync_whenUseBoundShopBound_expandsCsvDistributors() {
		SeckillActivityMapper activityMapper = Mockito.mock(SeckillActivityMapper.class);
		SeckillRelGoodsMapper relMapper = Mockito.mock(SeckillRelGoodsMapper.class);
		SalespersonItemsShelvesMapper shelvesMapper = Mockito.mock(SalespersonItemsShelvesMapper.class);

		SeckillActivity activity = baselineActivity();
		activity.setUseBound(1);
		activity.setDistributorId("7,8");

		when(activityMapper.selectById(ACTIVITY_ID)).thenReturn(activity);
		when(relMapper.selectList(any())).thenReturn(List.of(relRow(300L)));

		var contributor = new SeckillActivityItemsShelvesSyncContributor(activityMapper, relMapper, shelvesMapper);
		contributor.sync(COMPANY_ID, ACTIVITY_ID);

		ArgumentCaptor<SalespersonItemsShelves> cap = ArgumentCaptor.forClass(SalespersonItemsShelves.class);
		verify(shelvesMapper, times(2)).insert(cap.capture());
		List<Long> dist = cap.getAllValues().stream().map(SalespersonItemsShelves::getDistributorId).sorted().toList();
		assertEquals(List.of(7L, 8L), dist);
	}

	private static SeckillActivity baselineActivity() {
		int end = (int) (Instant.now().getEpochSecond() + 86_400);
		SeckillActivity a = new SeckillActivity();
		a.setSeckillId(ACTIVITY_ID);
		a.setCompanyId(COMPANY_ID);
		a.setSeckillType("normal");
		a.setActivityStartTime(1);
		a.setActivityEndTime(end);
		return a;
	}

	private static SeckillRelGoods relRow(long itemId) {
		SeckillRelGoods r = new SeckillRelGoods();
		r.setItemId(itemId);
		r.setDisabled(false);
		r.setIsShow(true);
		return r;
	}
}
