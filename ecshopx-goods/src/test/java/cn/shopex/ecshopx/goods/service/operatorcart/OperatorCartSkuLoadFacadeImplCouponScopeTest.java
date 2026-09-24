package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorCartSkuLoadFacadeImplCouponScopeTest {

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
	void loadCouponCartItemScopes_mergesSpuTagsOntoSku() {
		Items tagged = new Items();
		tagged.setItemId(2614L);
		tagged.setDefaultItemId(2614L);
		Items other = new Items();
		other.setItemId(2446L);
		other.setDefaultItemId(2446L);
		when(itemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tagged, other));
		ItemsRelTags rel = new ItemsRelTags();
		rel.setItemId(2614L);
		rel.setTagId(1001L);
		when(itemsRelTagsRepository.getLists(eq(1L), any())).thenReturn(List.of(rel));

		Map<Long, CouponCartItemScope> scopes = sut.loadCouponCartItemScopes(1L, List.of(2614L, 2446L));

		assertThat(scopes.get(2614L).tagIds()).isEqualTo(Set.of(1001L));
		assertThat(scopes.get(2446L).tagIds()).isEmpty();
	}
}
