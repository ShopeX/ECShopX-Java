package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsIsGiftBatchUpdateServiceTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	@InjectMocks
	private ItemsIsGiftBatchUpdateService service;

	@Test
	void supplierIsRejected() {
		assertThrows(
				ResourceException.class,
				() -> service.batchUpdateItemGift(38L, "supplier", List.of(337L), "true"));
		verify(itemsRepository, never()).updateIsGiftByCompanyAndGoodsId(anyLong(), anyLong(), anyBoolean());
	}

	@Test
	void setsGiftForEachGoodsId() {
		Items sku = new Items();
		sku.setItemId(1001L);
		when(itemsRepository.listByCompanyIdAndGoodsId(38L, 8472L, false)).thenReturn(List.of(sku));
		when(itemsRepository.listByCompanyIdAndGoodsId(38L, 9000L, false)).thenReturn(List.of(sku));

		service.batchUpdateItemGift(38L, "admin", List.of(8472L, 9000L), "true");

		verify(itemsRepository).updateIsGiftByCompanyAndGoodsId(eq(38L), eq(8472L), eq(true));
		verify(itemsRepository).updateIsGiftByCompanyAndGoodsId(eq(38L), eq(9000L), eq(true));
	}
}
