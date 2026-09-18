package cn.shopex.ecshopx.goods.service.recommend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class GoodsRecommendDisplaySettingValidatorTest {

	private final StaticMessageSource messageSource = new StaticMessageSource();

	@Test
	void validate_allDisabled_passesWithoutLimits() {
		Map<String, Object> input = baseInput();
		input.put("detail_enabled", 0);
		input.put("cart_enabled", 0);
		input.put("checkout_enabled", 0);
		input.put("order_detail_enabled", 0);
		assertDoesNotThrow(() -> GoodsRecommendDisplaySettingValidator.validate(input, messageSource));
	}

	@Test
	void validate_disabledPage_rejectsOutOfRangeLimitWhenProvided() {
		Map<String, Object> input = baseInput();
		input.put("detail_enabled", 0);
		input.put("detail_limit", 999);
		assertThrows(BadRequestException.class, () -> GoodsRecommendDisplaySettingValidator.validate(input, messageSource));
	}

	@Test
	void validate_enabledPage_requiresLimitInRange() {
		Map<String, Object> input = baseInput();
		input.put("detail_enabled", 1);
		input.put("detail_limit", 51);
		assertThrows(BadRequestException.class, () -> GoodsRecommendDisplaySettingValidator.validate(input, messageSource));
	}

	@Test
	void validate_enabledPage_acceptsValidSort() {
		Map<String, Object> input = baseInput();
		input.put("detail_enabled", 1);
		input.put("detail_limit", 10);
		input.put("detail_sort", GoodsRecommendDisplaySort.PRICE_ASC);
		assertDoesNotThrow(() -> GoodsRecommendDisplaySettingValidator.validate(input, messageSource));
	}

	@Test
	void defaults_matchSsot() {
		Map<String, Object> defaults = GoodsRecommendDisplayDefaults.defaultResponseMap(1L);
		assertEquals(0, defaults.get("detail_enabled"));
		assertEquals(6, defaults.get("detail_limit"));
		assertEquals(GoodsRecommendDisplaySort.SALES_DESC, defaults.get("detail_sort"));
		assertEquals(false, defaults.get("persisted"));
	}

	private static Map<String, Object> baseInput() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("detail_enabled", 0);
		input.put("cart_enabled", 0);
		input.put("checkout_enabled", 0);
		input.put("order_detail_enabled", 0);
		input.put("detail_limit", 6);
		input.put("cart_limit", 6);
		input.put("checkout_limit", 6);
		input.put("order_detail_limit", 6);
		input.put("detail_sort", GoodsRecommendDisplaySort.SALES_DESC);
		input.put("cart_sort", GoodsRecommendDisplaySort.SALES_DESC);
		input.put("checkout_sort", GoodsRecommendDisplaySort.SALES_DESC);
		input.put("order_detail_sort", GoodsRecommendDisplaySort.SALES_DESC);
		return input;
	}
}
