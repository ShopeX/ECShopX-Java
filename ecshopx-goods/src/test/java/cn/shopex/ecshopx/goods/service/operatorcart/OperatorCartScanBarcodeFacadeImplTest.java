package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
	private ItemsMapper itemsMapper;

	@InjectMocks
	private OperatorCartScanBarcodeFacadeImpl sut;

	@Test
	void resolve_usesDistributorScopedBarcode() {
		ItemsBarcode row = new ItemsBarcode();
		row.setItemId(901L);
		when(itemsBarcodeRepository.findFirstByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001"))
				.thenReturn(row);
		when(itemsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new Items());

		assertEquals(901L, sut.resolveItemIdForOperatorScan(7001L, 88L, "690001"));
	}

	@Test
	void resolve_missingBarcode_throws() {
		when(itemsBarcodeRepository.findFirstByCompanyIdAndDistributorIdAndBarcode(7001L, 88L, "690001"))
				.thenReturn(null);
		assertThrows(ResourceException.class, () -> sut.resolveItemIdForOperatorScan(7001L, 88L, "690001"));
	}
}
