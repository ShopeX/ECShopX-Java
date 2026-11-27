package cn.shopex.ecshopx.goods.service.cart.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WxappH5CartListServiceCheckoutOffShelfTipTest {

	@Test
	void allInvalidOffShelf_sameValidShop_usesOffShelfMessage() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("shop_id", 10L);
		line.put("approve_status", "instock");
		assertTrue(
				WxappH5CartListService.allInvalidCartLinesAreOffShelf(
						List.of(line), Set.of(10L), "distributor"));
		assertEquals(
				"商品已下架",
				WxappH5CartListService.messageWhenCheckoutValidCartEmpty(true));
	}

	@Test
	void invalidBecauseZeroStore_keepsSameShopMessage() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("shop_id", 10L);
		line.put("approve_status", "onsale");
		assertFalse(
				WxappH5CartListService.allInvalidCartLinesAreOffShelf(
						List.of(line), Set.of(10L), "distributor"));
		assertEquals(
				"购物车商品不是来自同一个店铺",
				WxappH5CartListService.messageWhenCheckoutValidCartEmpty(false));
	}

	@Test
	void invalidBecauseShopNotValid_keepsSameShopMessage() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("shop_id", 99L);
		line.put("approve_status", "instock");
		assertFalse(
				WxappH5CartListService.allInvalidCartLinesAreOffShelf(
						List.of(line), Set.of(10L), "distributor"));
	}

	@Test
	void invalidWithoutApproveStatus_keepsSameShopMessage() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("shop_id", 10L);
		assertFalse(
				WxappH5CartListService.allInvalidCartLinesAreOffShelf(
						List.of(line), Set.of(10L), "distributor"));
	}
}
