package cn.shopex.ecshopx.goods.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappGoodsItemsBatchServiceTest {

	private static final long COMPANY_ID = 141L;

	@Mock
	private ItemsRepository itemsRepository;
	@Mock
	private ItemsListMultiLangApplier itemsListMultiLangApplier;

	private WxappGoodsItemsBatchService sut;

	@BeforeEach
	void setUp() {
		sut = new WxappGoodsItemsBatchService(itemsRepository, itemsListMultiLangApplier);
	}

	@Test
	void execute_emptyItemIds_returnsEmptyWithoutQuery() {
		assertTrue(sut.execute(COMPANY_ID, "", "zh-CN").isEmpty());
		assertTrue(sut.execute(COMPANY_ID, "  ", "zh-CN").isEmpty());
		verifyNoInteractions(itemsRepository, itemsListMultiLangApplier);
	}

	@Test
	void execute_invalidItemIds_returnsEmptyWithoutQuery() {
		assertTrue(sut.execute(COMPANY_ID, "abc,0,-1", "zh-CN").isEmpty());
		verifyNoInteractions(itemsRepository, itemsListMultiLangApplier);
	}

	@Test
	void execute_queriesLightFieldsByItemIds() {
		Items item1 = item(1L, "商品A", 1000, "[\"/a.jpg\"]");
		Items item2 = item(2L, "商品B", 2000, "[\"/b.jpg\"]");
		when(itemsRepository.listByCompanyAndItemIdsPreservingOrder(COMPANY_ID, List.of(1L, 2L, 3L)))
				.thenReturn(List.of(item1, item2));

		List<Map<String, Object>> result = sut.execute(COMPANY_ID, "1,2,3", "zh-CN");

		assertEquals(2, result.size());
		assertEquals(1L, result.get(0).get("item_id"));
		assertEquals("商品A", result.get(0).get("item_name"));
		assertEquals("商品A", result.get(0).get("itemName"));
		assertEquals(1000, result.get(0).get("price"));
		assertEquals(List.of("/a.jpg"), result.get(0).get("pics"));
		assertEquals(2L, result.get(1).get("item_id"));

		ArgumentCaptor<List<Map<String, Object>>> rowsCaptor = ArgumentCaptor.forClass(List.class);
		verify(itemsListMultiLangApplier).applyToRows(eq(COMPANY_ID), eq("zh-CN"), rowsCaptor.capture());
		assertEquals(2, rowsCaptor.getValue().size());
	}

	@Test
	void parseItemIds_matchesPhpIntvalSemantics() {
		assertEquals(List.of(8087L), WxappGoodsItemsBatchService.parseItemIds("8087"));
		assertEquals(List.of(1L, 2L, 3L), WxappGoodsItemsBatchService.parseItemIds("1,2,3"));
		assertEquals(List.of(1L), WxappGoodsItemsBatchService.parseItemIds("1.9,abc,0"));
		assertTrue(WxappGoodsItemsBatchService.parseItemIds("").isEmpty());
	}

	private static Items item(long itemId, String name, int price, String picsJson) {
		Items it = new Items();
		it.setItemId(itemId);
		it.setItemName(name);
		it.setPrice(price);
		it.setPics(picsJson);
		return it;
	}
}
