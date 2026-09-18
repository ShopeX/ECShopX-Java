package cn.shopex.ecshopx.goods.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsTagsQueryService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCardIdsByGoodsForListService;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class WxappGoodsItemsListParamBuilderCategoryTest {

	@Mock
	private ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	@Mock
	private ItemsListQueryRepository itemsListQueryRepository;
	@Mock
	private ItemsTagsQueryService itemsTagsQueryService;
	@Mock
	private DiscountCardCardIdsByGoodsForListService discountCardCardIdsByGoodsForListService;

	private WxappGoodsItemsListParamBuilder sut;

	@BeforeEach
	void setUp() {
		sut = new WxappGoodsItemsListParamBuilder(itemsCategoryItemIdResolver, itemsListQueryRepository,
				itemsTagsQueryService, discountCardCardIdsByGoodsForListService);
	}

	@Test
	void build_emptyCategoryTree_forcesNoMatchItemIds() {
		when(itemsCategoryItemIdResolver.getItemIdsByCategoryTree(1L, 909L)).thenReturn(List.of());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("category_id", "909");

		LinkedHashMap<String, Object> params = sut.build(request, 1L, 0L);

		assertEquals(List.of(-1L), params.get("category_resolved_item_ids"));
	}
}
