package cn.shopex.ecshopx.goods.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.mapper.ItemsBarcodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsBarcodeRepositoryShopScopedTest {

	@Mock
	private ItemsBarcodeMapper mapper;

	@InjectMocks
	private ItemsBarcodeRepository sut;

	@Test
	void saveBarcode_sameShopDuplicate_throws() {
		when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
		when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

		ResourceException ex =
				assertThrows(ResourceException.class, () -> sut.saveBarcode(7001L, 88L, 100L, 100L, "690001"));
		assertEquals("条形码已经存在", ex.getMessage());
		verify(mapper, never()).insert(any(ItemsBarcode.class));
	}

	@Test
	void saveBarcode_differentShopSameCode_inserts() {
		when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
		when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

		sut.saveBarcode(7001L, 88L, 100L, 100L, "690001");

		ArgumentCaptor<ItemsBarcode> cap = ArgumentCaptor.forClass(ItemsBarcode.class);
		verify(mapper, times(1)).insert(cap.capture());
		ItemsBarcode row = cap.getValue();
		assertEquals(7001L, row.getCompanyId());
		assertEquals(88L, row.getDistributorId());
		assertEquals(100L, row.getItemId());
		assertEquals("690001", row.getBarcode());
	}

	@Test
	void saveBarcode_platformPool_usesDistributorZero() {
		when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
		when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

		sut.saveBarcode(7001L, 0L, 200L, 200L, "690002");

		ArgumentCaptor<ItemsBarcode> cap = ArgumentCaptor.forClass(ItemsBarcode.class);
		verify(mapper).insert(cap.capture());
		assertEquals(0L, cap.getValue().getDistributorId());
	}
}
