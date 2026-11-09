package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorCartSkuLoadFacadeImplLoadSkusTest {

	@Mock
	private ItemsMapper itemsMapper;

	@Mock
	private DistributorItemsMapper distributorItemsMapper;

	@Mock
	private ItemsRelTagsRepository itemsRelTagsRepository;

	@Mock
	private OperatorCartMemberPriceApplicator operatorCartMemberPriceApplicator;

	@Mock
	private ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;

	@InjectMocks
	private OperatorCartSkuLoadFacadeImpl sut;

	@Test
	void loadSkus_skipsMissingItemsWithoutThrowing() {
		Items existing = new Items();
		existing.setItemId(10L);
		existing.setDefaultItemId(10L);
		existing.setPrice(100);
		existing.setStore(5);
		when(itemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));
		when(itemLogisticsStoreEnricher.combineEffectiveStore(eq(1L), eq(2L), eq(5), any(Items.class)))
				.thenReturn(5);

		var rows = sut.loadSkus(1L, 2L, 0L, List.of(10L, 99L));

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getItemId()).isEqualTo(10L);
	}

	@Test
	void loadSkus_allMissing_returnsEmptyList() {
		when(itemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		var rows = sut.loadSkus(1L, 2L, 0L, List.of(99L));

		assertThat(rows).isEmpty();
	}
}
