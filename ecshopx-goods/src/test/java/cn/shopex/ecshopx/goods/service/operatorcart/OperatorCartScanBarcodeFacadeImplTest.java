package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorCartScanBarcodeFacadeImplTest {

	@Mock
	private ItemsBarcodeRepository itemsBarcodeRepository;

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemsMapper itemsMapper;

	@InjectMocks
	private OperatorCartScanBarcodeFacadeImpl sut;

	@Test
	void resolve_usesDistributorScopedBarcode() {
		ItemsBarcode row = new ItemsBarcode();
		row.setItemId(901L);
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001"))
				.thenReturn(List.of(row));
		when(itemsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new Items());

		assertEquals(901L, sut.resolveItemIdForOperatorScan(7001L, 88L, "690001"));
	}

	@Test
	void resolve_usesDistributorScopedItemsBarcodeColumn() {
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001"))
				.thenReturn(List.of());
		Items item = new Items();
		item.setItemId(902L);
		item.setDistributorId(88);
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(7001L, 88L, "690001"))
				.thenReturn(List.of(item));
		when(itemsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

		assertEquals(902L, sut.resolveItemIdForOperatorScan(7001L, 88L, "690001"));
	}

	@Test
	void resolve_fallsBackToPlatformItemsBarcode_whenStoreMiss() {
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 306L, "6947012851098"))
				.thenReturn(List.of());
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(7001L, 306L, "6947012851098"))
				.thenReturn(List.of());
		ItemsBarcode hq = new ItemsBarcode();
		hq.setItemId(1001L);
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 0L, "6947012851098"))
				.thenReturn(List.of(hq));
		when(itemsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new Items());

		assertEquals(1001L, sut.resolveItemIdForOperatorScan(7001L, 306L, "6947012851098"));
	}

	@Test
	void resolve_fallsBackToPlatformItemsBarcodeColumn_whenStoreAndExtMiss() {
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(eq(7001L), eq(306L), eq("6947012851098")))
				.thenReturn(List.of());
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(7001L, 306L, "6947012851098"))
				.thenReturn(List.of());
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(eq(7001L), eq(0L), eq("6947012851098")))
				.thenReturn(List.of());
		Items hq = new Items();
		hq.setItemId(1002L);
		hq.setDistributorId(0);
		when(itemsRepository.listByCompanyIdAndBarcodeExact(7001L, "6947012851098")).thenReturn(List.of(hq));
		when(itemsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(hq);

		assertEquals(1002L, sut.resolveItemIdForOperatorScan(7001L, 306L, "6947012851098"));
	}

	@Test
	void resolve_missingBarcode_throws() {
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001"))
				.thenReturn(List.of());
		when(itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(7001L, 88L, "690001"))
				.thenReturn(List.of());
		when(itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(7001L, 0L, "690001"))
				.thenReturn(List.of());
		when(itemsRepository.listByCompanyIdAndBarcodeExact(7001L, "690001")).thenReturn(List.of());

		assertThrows(ResourceException.class, () -> sut.resolveItemIdForOperatorScan(7001L, 88L, "690001"));
	}
}
