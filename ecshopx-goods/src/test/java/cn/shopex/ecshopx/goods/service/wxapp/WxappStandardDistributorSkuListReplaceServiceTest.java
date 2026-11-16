package cn.shopex.ecshopx.goods.service.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonCartDistributorSkuReplaceService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappStandardDistributorSkuListReplaceServiceTest {

	@Mock ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	@Mock SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService;

	private WxappStandardDistributorSkuListReplaceService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappStandardDistributorSkuListReplaceService(
						itemsCategoryDistributorIdResolver, salespersonCartDistributorSkuReplaceService);
	}

	@Test
	void applyIfNeeded_standardWithStoreOnsale_delegatesToSkuReplace() {
		when(itemsCategoryDistributorIdResolver.resolveProductModel(38L)).thenReturn("standard");
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("is_can_sale", Boolean.TRUE);
		params.put("distributor_id", 3370L);
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 8254L);
		row.put("price", 10000);
		row.put("store", 3);
		List<Map<String, Object>> list = List.of(row);

		service.applyIfNeeded(38L, params, list);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Map<String, Object>>> listCaptor = ArgumentCaptor.forClass(List.class);
		verify(salespersonCartDistributorSkuReplaceService).apply(eq(38L), eq(3370L), listCaptor.capture());
		assertThat(listCaptor.getValue()).isSameAs(list);
		assertThat(row.get("v_store")).isEqualTo(1);
	}

	@Test
	void applyIfNeeded_skipsWhenIsCanSaleMissing() {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("distributor_id", 3370L);
		List<Map<String, Object>> list = List.of(Map.of("item_id", 1L));

		service.applyIfNeeded(38L, params, list);

		verify(salespersonCartDistributorSkuReplaceService, org.mockito.Mockito.never()).apply(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
	}
}