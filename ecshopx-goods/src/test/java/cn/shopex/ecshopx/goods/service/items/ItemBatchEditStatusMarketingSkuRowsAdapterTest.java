package cn.shopex.ecshopx.goods.service.items;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemBatchEditStatusMarketingSkuRowsAdapterTest {

	@Mock
	private ItemsRepository itemsRepository;

	private ItemBatchEditStatusMarketingSkuRowsAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new ItemBatchEditStatusMarketingSkuRowsAdapter(itemsRepository);
	}

	@Test
	void listRows_mapsItemBnAndApproveStatusFromRepositoryRows() {
		Items a = new Items();
		a.setItemBn("SKU-1");
		a.setApproveStatus("onsale");
		Items b = new Items();
		b.setItemBn("SKU-2");
		b.setApproveStatus("instock");

		when(itemsRepository.listByCompanyIdAndGoodsId(9L, 100L, false)).thenReturn(List.of(a, b));

		List<Map<String, Object>> rows = adapter.listRows(9L, 100L);

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0)).containsEntry("item_bn", "SKU-1").containsEntry("approve_status", "onsale");
		assertThat(rows.get(1)).containsEntry("item_bn", "SKU-2").containsEntry("approve_status", "instock");
	}
}
