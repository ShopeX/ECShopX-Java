package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsApproveStatusSyncServiceTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private DistributorItemsRepository distributorItemsRepository;

	private ItemsApproveStatusSyncService service;

	@BeforeEach
	void setUp() {
		service = new ItemsApproveStatusSyncService(itemsRepository, distributorItemsRepository);
	}

	@Test
	void sync_emptyItems_noDistributorUpdate() {
		when(itemsRepository.listByCompanyIdAndGoodsId(1L, 2L, false)).thenReturn(List.of());
		service.syncDistributorItemsForApproveStatus(1L, 2L, "onsale");
		verify(distributorItemsRepository, never()).updateIsCanSaleAndUpdatedByCompanyAndItemIds(anyLong(), any(), any(), anyLong());
	}

	@Test
	void sync_onsale_setsCanSaleTrueAndUpdated() {
		Items a = new Items();
		a.setItemId(10L);
		when(itemsRepository.listByCompanyIdAndGoodsId(5L, 7L, false)).thenReturn(List.of(a));
		service.syncDistributorItemsForApproveStatus(5L, 7L, "onsale");
		ArgumentCaptor<Long> updatedCap = ArgumentCaptor.forClass(Long.class);
		verify(distributorItemsRepository)
				.updateIsCanSaleAndUpdatedByCompanyAndItemIds(
						eq(5L), eq(List.of(10L)), eq(Boolean.TRUE), updatedCap.capture());
		long now = System.currentTimeMillis() / 1000L;
		assertTrue(Math.abs(now - updatedCap.getValue()) <= 2);
	}

	@Test
	void sync_instock_setsCanSaleFalseAndUpdated() {
		Items a = new Items();
		a.setItemId(20L);
		when(itemsRepository.listByCompanyIdAndGoodsId(3L, 4L, false)).thenReturn(List.of(a));
		service.syncDistributorItemsForApproveStatus(3L, 4L, "instock");
		verify(distributorItemsRepository)
				.updateIsCanSaleAndUpdatedByCompanyAndItemIds(eq(3L), eq(List.of(20L)), eq(Boolean.FALSE), anyLong());
	}

	@Test
	void sync_otherStatus_onlyUpdated() {
		Items a = new Items();
		a.setItemId(30L);
		when(itemsRepository.listByCompanyIdAndGoodsId(8L, 9L, false)).thenReturn(List.of(a));
		service.syncDistributorItemsForApproveStatus(8L, 9L, "draft");
		verify(distributorItemsRepository)
				.updateIsCanSaleAndUpdatedByCompanyAndItemIds(eq(8L), eq(List.of(30L)), isNull(), anyLong());
	}
}
