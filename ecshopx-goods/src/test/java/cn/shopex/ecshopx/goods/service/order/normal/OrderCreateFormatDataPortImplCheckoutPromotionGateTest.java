package cn.shopex.ecshopx.goods.service.order.normal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * POS 购物车来源单执行结算促销合并的判定（ECX-10719）：
 * 小程序在线普通单维持原有合并；POS/shopadmin 购物车来源单（operator_id &gt; 0）同样执行，
 * 使购物车自动应用的满减、满折、限时特惠落到订单。
 */
class OrderCreateFormatDataPortImplCheckoutPromotionGateTest {

	@Test
	void shouldMergeCheckoutPromotion_wxappOnlineNormalAlwaysTrue() {
		Map<String, Object> pr = new LinkedHashMap<>();
		pr.put("operator_id", 0L);

		assertTrue(OrderCreateFormatDataPortImpl.shouldMergeCheckoutPromotion(true, pr));
		assertTrue(OrderCreateFormatDataPortImpl.shouldMergeCheckoutPromotion(true, pr));
	}

	@Test
	void shouldMergeCheckoutPromotion_operatorCartCheckoutTrue() {
		Map<String, Object> pr = new LinkedHashMap<>();
		pr.put("operator_id", 21L);

		assertTrue(OrderCreateFormatDataPortImpl.shouldMergeCheckoutPromotion(false, pr));
	}

	@Test
	void shouldMergeCheckoutPromotion_noOperatorFalse() {
		Map<String, Object> pr = new LinkedHashMap<>();

		assertFalse(OrderCreateFormatDataPortImpl.shouldMergeCheckoutPromotion(false, pr));

		pr.put("operator_id", 0L);
		assertFalse(OrderCreateFormatDataPortImpl.shouldMergeCheckoutPromotion(false, pr));
	}
}
