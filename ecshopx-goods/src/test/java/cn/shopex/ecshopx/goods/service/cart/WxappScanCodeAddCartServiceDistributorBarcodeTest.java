package cn.shopex.ecshopx.goods.service.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappScanCodeAddCartServiceDistributorBarcodeTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemsBarcodeRepository itemsBarcodeRepository;

	@Mock
	private WxappDistributorCartAddCoreService wxappDistributorCartAddCoreService;

	@InjectMocks
	private WxappScanCodeAddCartService sut;

	@Test
	void execute_resolvesViaItemsBarcodeWithDistributorId() {
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(7001L, 88L, "690001"))
				.thenReturn(List.of());
		ItemsBarcode row = new ItemsBarcode();
		row.setItemId(501L);
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001"))
				.thenReturn(List.of(row));
		when(wxappDistributorCartAddCoreService.addCart(
						eq(7001L), eq(0L), eq(501L), eq("distributor"), eq("normal"), eq(1), eq(true), eq(88L), eq(null)))
				.thenReturn(Map.of());

		Map<String, Object> result = sut.execute(7001L, 0L, Map.of("barcode", "690001", "distributor_id", "88"));

		assertEquals(501L, result.get("item_id"));
		verify(itemsBarcodeRepository).listByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001");
	}

	@Test
	void execute_noMatchForShop_throws() {
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(anyLong(), anyLong(), anyString()))
				.thenReturn(List.of());
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(anyLong(), anyLong(), anyString()))
				.thenReturn(List.of());

		assertThrows(
				ResourceException.class,
				() -> sut.execute(7001L, 1L, Map.of("barcode", "690001", "distributor_id", "88")));
	}

	@Test
	void execute_matchesItemsBarcodeColumnWithDistributor() {
		Items item = new Items();
		item.setItemId(502L);
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(7001L, 88L, "690001"))
				.thenReturn(List.of(item));
		when(wxappDistributorCartAddCoreService.addCart(
						eq(7001L), eq(0L), eq(502L), eq("distributor"), eq("normal"), eq(1), eq(true), eq(88L), eq(null)))
				.thenReturn(Map.of());

		Map<String, Object> result = sut.execute(7001L, 0L, Map.of("barcode", "690001", "distributor_id", 88));

		assertEquals(502L, result.get("item_id"));
	}
}
