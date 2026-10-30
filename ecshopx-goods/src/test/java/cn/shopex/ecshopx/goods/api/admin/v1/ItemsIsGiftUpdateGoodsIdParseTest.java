package cn.shopex.ecshopx.goods.api.admin.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemsIsGiftUpdateGoodsIdParseTest {

	@Test
	void parsesPhpStyleIndexedObjectFromFrontendObjectAssign() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("0", 8472);
		assertEquals(List.of(8472L), ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(body));
	}

	@Test
	void parsesPhpStyleIndexedObjectWithStringValues() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("0", "337");
		assertEquals(List.of(337L), ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(body));
	}

	@Test
	void parsesMultipleIndexedObjectValues() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("0", 11);
		body.put("1", "22");
		assertEquals(List.of(11L, 22L), ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(body));
	}

	@Test
	void parsesScalarNumberAndStringAndList() {
		assertEquals(List.of(9L), ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(9));
		assertEquals(List.of(9L), ItemsController.parseRequiredGoodsIdsForIsGiftUpdate("9"));
		assertEquals(List.of(1L, 2L), ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(List.of(1, "2")));
	}

	@Test
	void rejectsEmptyAndInvalid() {
		assertThrows(BadRequestException.class, () -> ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(null));
		assertThrows(BadRequestException.class, () -> ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(Map.of()));
		assertThrows(
				BadRequestException.class, () -> ItemsController.parseRequiredGoodsIdsForIsGiftUpdate(Map.of("0", "x")));
	}
}
