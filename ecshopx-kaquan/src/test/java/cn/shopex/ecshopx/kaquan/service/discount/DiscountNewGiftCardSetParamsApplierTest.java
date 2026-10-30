package cn.shopex.ecshopx.kaquan.service.discount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscountNewGiftCardSetParamsApplierTest {

	private final DiscountNewGiftCardSetParamsApplier applier =
			new DiscountNewGiftCardSetParamsApplier(new ObjectMapper());

	@Test
	@DisplayName("distributor_ids 字符串 []：全店铺，写入空 distributor_id + use_all_shops")
	void emptyDistributorIdsJsonString_appliesAllShops() {
		Map<String, Object> params = new HashMap<>();
		params.put("distributor_ids", "[]");
		params.put("coupon_type", "mall");

		applier.apply(params, false);

		assertFalse(params.containsKey("distributor_ids"));
		assertEquals("true", params.get("use_all_shops"));
		assertInstanceOf(List.class, params.get("distributor_id"));
		assertTrue(((List<?>) params.get("distributor_id")).isEmpty());
	}

	@Test
	@DisplayName("distributor_ids 空 List：全店铺，写入空 distributor_id + use_all_shops")
	void emptyDistributorIdsList_appliesAllShops() {
		Map<String, Object> params = new HashMap<>();
		params.put("distributor_ids", List.of());
		params.put("coupon_type", "mall");

		applier.apply(params, false);

		assertFalse(params.containsKey("distributor_ids"));
		assertEquals("true", params.get("use_all_shops"));
		assertInstanceOf(List.class, params.get("distributor_id"));
		assertTrue(((List<?>) params.get("distributor_id")).isEmpty());
	}

	@Test
	@DisplayName("distributor_ids 非空：指定店铺，use_all_shops=false")
	void nonEmptyDistributorIds_appliesSpecificShops() {
		Map<String, Object> params = new HashMap<>();
		params.put("distributor_ids", List.of(12, 34));
		params.put("coupon_type", "mall");

		applier.apply(params, false);

		assertFalse(params.containsKey("distributor_ids"));
		assertEquals("false", params.get("use_all_shops"));
		assertEquals(List.of(12, 34), params.get("distributor_id"));
	}
}
