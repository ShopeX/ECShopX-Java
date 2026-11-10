package cn.shopex.ecshopx.goods.service.operatorcart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.kaquan.service.order.normal.NormalOrderCheckoutCouponFacade;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * POS 购物车券后预览合并（ECX-11082）：kaquan 层完成券校验/行分摊后，
 * 同步更新店铺级 coupon_discount_fee / discount_fee（活动+券）/ total_fee 与 coupon_info。
 * 券不可用（deduct_fen=0）时不改任何字段；仅预览，不影响下单链路。
 */
@ExtendWith(MockitoExtension.class)
class OperatorCartPosCouponServiceTest {

	private static final long COMPANY_ID = 38L;
	private static final long OPERATOR_ID = 21L;
	private static final long USER_ID = 3850L;
	private static final long SHOP_ID = 129L;

	@Mock private NormalOrderCheckoutCouponFacade checkoutCouponFacade;

	private OperatorCartPosCouponService service;

	@BeforeEach
	void setUp() {
		service = new OperatorCartPosCouponService(checkoutCouponFacade);
	}

	@Test
	void applyCouponPreview_updatesShopTotalsWithCouponDeduction() {
		Map<String, Object> cartData = cartData(3000L, 37000L);
		Map<String, Object> couponOut = new LinkedHashMap<>();
		couponOut.put("deduct_fen", 500L);
		couponOut.put("coupon_info", Map.of("type", "cash_discount", "discount_fee", "500"));
		when(checkoutCouponFacade.previewCouponDeduction(
						eq(COMPANY_ID), eq(OPERATOR_ID), eq(USER_ID), eq(SHOP_ID), eq("POS-CASH-001"), any()))
				.thenReturn(couponOut);

		service.applyCouponPreview(COMPANY_ID, OPERATOR_ID, USER_ID, cartData, "POS-CASH-001");

		Map<String, Object> shop = shopOf(cartData);
		assertEquals(500L, ((Number) shop.get("coupon_discount_fee")).longValue());
		assertEquals(3500L, ((Number) shop.get("discount_fee")).longValue());
		assertEquals(36500L, ((Number) shop.get("total_fee")).longValue());
		assertEquals(couponOut.get("coupon_info"), shop.get("coupon_info"));
	}

	@Test
	void applyCouponPreview_deductZeroKeepsShopTotalsUntouched() {
		Map<String, Object> cartData = cartData(3000L, 37000L);
		when(checkoutCouponFacade.previewCouponDeduction(
						anyLong(), anyLong(), anyLong(), anyLong(), anyString(), any()))
				.thenReturn(Map.of("deduct_fen", 0L));

		service.applyCouponPreview(COMPANY_ID, OPERATOR_ID, USER_ID, cartData, "POS-CASH-001");

		Map<String, Object> shop = shopOf(cartData);
		assertEquals(3000L, ((Number) shop.get("discount_fee")).longValue());
		assertEquals(37000L, ((Number) shop.get("total_fee")).longValue());
		assertFalse(shop.containsKey("coupon_discount_fee"));
		assertFalse(shop.containsKey("coupon_info"));
	}

	@Test
	void applyCouponPreview_skipsForEmptyCodeOrNoMember() {
		Map<String, Object> cartData = cartData(3000L, 37000L);

		service.applyCouponPreview(COMPANY_ID, OPERATOR_ID, USER_ID, cartData, "");
		service.applyCouponPreview(COMPANY_ID, OPERATOR_ID, 0L, cartData, "POS-CASH-001");
		service.applyCouponPreview(COMPANY_ID, OPERATOR_ID, USER_ID, cartData, null);

		verify(checkoutCouponFacade, never()).previewCouponDeduction(
				anyLong(), anyInt(), anyLong(), anyLong(), anyString(), any());
	}

	@Test
	void applyCouponPreview_linesPassedToFacadeWithShopScope() {
		Map<String, Object> cartData = cartData(3000L, 37000L);
		List<Map<String, Object>> lines = shopOf(cartData).get("list") instanceof List<?> l
				? castLines(l)
				: List.of();
		when(checkoutCouponFacade.previewCouponDeduction(
						anyLong(), anyLong(), anyLong(), anyLong(), anyString(), any()))
				.thenReturn(Map.of("deduct_fen", 0L));

		service.applyCouponPreview(COMPANY_ID, OPERATOR_ID, USER_ID, cartData, "POS-CASH-001");

		verify(checkoutCouponFacade).previewCouponDeduction(
				eq(COMPANY_ID), eq(OPERATOR_ID), eq(USER_ID), eq(SHOP_ID), eq("POS-CASH-001"), eq(lines));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castLines(List<?> l) {
		return (List<Map<String, Object>>) l;
	}

	private static Map<String, Object> cartData(long discountFee, long totalFee) {
		Map<String, Object> lineA = new LinkedHashMap<>();
		lineA.put("cart_id", 631L);
		lineA.put("item_id", 7666L);
		lineA.put("num", 2);
		lineA.put("total_fee", "36815");
		Map<String, Object> shop = new LinkedHashMap<>();
		shop.put("shop_id", SHOP_ID);
		shop.put("item_fee", 40000L);
		shop.put("discount_fee", discountFee);
		shop.put("total_fee", totalFee);
		shop.put("list", new ArrayList<>(List.of(lineA)));
		Map<String, Object> cartData = new LinkedHashMap<>();
		cartData.put("valid_cart", new ArrayList<>(List.of(shop)));
		cartData.put("invalid_cart", List.of());
		return cartData;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> shopOf(Map<String, Object> cartData) {
		return ((List<Map<String, Object>>) cartData.get("valid_cart")).get(0);
	}
}
