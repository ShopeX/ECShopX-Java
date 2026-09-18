package cn.shopex.ecshopx.goods.service.order.normal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.deposit.service.UserDepositBalanceReadService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.kaquan.service.order.normal.NormalOrderCheckoutCouponFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

/**
 * PHP OrderService::_formatOrderData shop_id comes from shopInfo (default 0) when isCheckShopValid
 * is false for normal/shopadmin — not from distributor_id.
 */
@ExtendWith(MockitoExtension.class)
class OrderCreateFormatDataPortImplShopIdAlignPhpTest {

	@Mock
	private OrderDirectedCrowdDiscountPort orderDirectedCrowdDiscountPort;

	@Mock
	private NormalOrderCheckoutCouponFacade normalOrderCheckoutCouponFacade;

	@Mock
	private UserDepositBalanceReadService userDepositBalanceReadService;

	@Mock
	private WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;

	@Mock
	private OrderCheckoutFullGiftService orderCheckoutFullGiftService;

	@Mock
	private OrderCheckoutPlusBuyService orderCheckoutPlusBuyService;

	@Mock
	private MessageSource messageSource;

	private OrderCreateFormatDataPortImpl port;

	@BeforeEach
	void setUp() {
		port =
				new OrderCreateFormatDataPortImpl(
						orderDirectedCrowdDiscountPort,
						normalOrderCheckoutCouponFacade,
						userDepositBalanceReadService,
						wxappGoodsItemsListQueryOrchestrator,
						orderCheckoutFullGiftService,
						orderCheckoutPlusBuyService,
						messageSource);
		doNothing().when(normalOrderCheckoutCouponFacade).applyOptimalCouponAndSilentDeduction(any());
		doNothing().when(orderDirectedCrowdDiscountPort).applySetUserTotalDiscountIfNeeded(any());
	}

	@Test
	void format_setsShopIdToZero_notDistributorId() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> pr = p.getParams();
		pr.put("company_id", 141L);
		pr.put("user_id", 7L);
		pr.put("mobile", "13800000000");
		pr.put("distributor_id", 104L);
		pr.put("order_type", "normal_shopadmin");
		pr.put("receipt_type", "ziti");
		pr.put("order_source", "shop_offline");
		pr.put("pay_type", "pos");
		pr.put("is_online_order", Boolean.FALSE);
		Map<String, Object> cartLine = new LinkedHashMap<>();
		cartLine.put("item_id", 11L);
		cartLine.put("price", 100);
		cartLine.put("item_name", "sku");
		cartLine.put("shop_id", 104L);
		pr.put("_checkout_cart_meta", Map.of("list", List.of(cartLine)));
		pr.put("items", List.of(Map.of("item_id", 11L, "num", 1)));

		port.format(p);

		Map<String, Object> od = p.getOrderData();
		assertThat(od.get("distributor_id")).isEqualTo(104L);
		assertThat(od.get("shop_id")).isEqualTo(0L);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) od.get("items");
		assertThat(items).hasSize(1);
		assertThat(items.get(0).get("shop_id")).isEqualTo(0L);
	}
}
