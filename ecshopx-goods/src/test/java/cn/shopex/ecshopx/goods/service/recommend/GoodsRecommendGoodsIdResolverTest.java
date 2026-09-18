package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsRecommendGoodsIdResolverTest {

	@Mock
	private ItemsMapper itemsMapper;

	private GoodsRecommendGoodsIdResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new GoodsRecommendGoodsIdResolver(itemsMapper);
	}

	@Test
	void resolveGoodsId_prefersGoodsId() {
		Items item = new Items();
		item.setItemId(6753L);
		item.setGoodsId(6749L);
		assertEquals(6749L, resolver.resolveGoodsId(item));
	}

	@Test
	void resolveGoodsId_fallsBackToItemId() {
		Items item = new Items();
		item.setItemId(100L);
		item.setGoodsId(0L);
		assertEquals(100L, resolver.resolveGoodsId(item));
	}

	@Test
	void loadDisplayItemsByGoodsId_prefersDefaultSku() {
		Items defaultSku = new Items();
		defaultSku.setItemId(6753L);
		defaultSku.setGoodsId(6749L);
		defaultSku.setIsDefault(true);

		Items otherSku = new Items();
		otherSku.setItemId(6750L);
		otherSku.setGoodsId(6749L);
		otherSku.setIsDefault(false);

		when(itemsMapper.selectList(any())).thenReturn(List.of(defaultSku, otherSku));

		Items display = resolver.loadDisplayItemsByGoodsId(1L, List.of(6749L)).get(6749L);
		assertEquals(6753L, display.getItemId());
	}
}
