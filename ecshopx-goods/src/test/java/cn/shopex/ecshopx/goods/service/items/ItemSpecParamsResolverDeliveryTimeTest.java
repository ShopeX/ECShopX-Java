package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemSpecParamsResolverDeliveryTimeTest {

	private ItemSpecParamsResolver sut;

	@BeforeEach
	void setUp() {
		sut = new ItemSpecParamsResolver(
				mock(ItemsCategoryRepository.class),
				mock(ItemsAttributesRepository.class),
				mock(ItemsAttributeValuesRepository.class),
				mock(ShippingTemplatesQueryRepository.class));
	}

	@Test
	void resolve_acceptsIntegerDeliveryTimeDays() {
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> sku = baseSku();
		sku.put("delivery_time", "2");
		ItemsCreateContext ctx = new ItemsCreateContext(1L, "supplier", "normal", false, true);

		sut.resolve(data, sku, ctx);

		assertEquals(2, data.get("delivery_time"));
	}

	@Test
	void resolve_rejectsNonNumericDeliveryTimeWithBusinessError() {
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> sku = baseSku();
		sku.put("delivery_time", "48小时");
		ItemsCreateContext ctx = new ItemsCreateContext(1L, "supplier", "normal", false, true);

		ResourceException ex = assertThrows(ResourceException.class, () -> sut.resolve(data, sku, ctx));
		assertEquals("发货时间格式错误，请填写非负整数天数（如 2）", ex.getMessage());
	}

	private static Map<String, Object> baseSku() {
		Map<String, Object> sku = new LinkedHashMap<>();
		sku.put("approve_status", "onsale");
		sku.put("item_bn", "BN1");
		sku.put("weight", 1);
		sku.put("barcode", "");
		sku.put("price", "10");
		return sku;
	}
}
