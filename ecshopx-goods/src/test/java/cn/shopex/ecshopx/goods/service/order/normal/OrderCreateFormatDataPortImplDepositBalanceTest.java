package cn.shopex.ecshopx.goods.service.order.normal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.deposit.service.UserDepositBalanceReadService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.kaquan.service.order.normal.NormalOrderCheckoutCouponFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

/**
 * ECX-10469：储值（pay_type=deposit）余额校验只允许发生在真实创建订单路径
 * （{@link OrderCreateFormatDataPortImpl#assertDepositBalance}），
 * 结算页预览（getFreightFee / getOrderTempInfo / shopadmin checkout）复用 format()
 * 时不得提前抛「余额不足」，商品清单需正常返回。
 */
@ExtendWith(MockitoExtension.class)
class OrderCreateFormatDataPortImplDepositBalanceTest {

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

	private NormalOrderCreateParams depositParams() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> pr = p.getParams();
		pr.put("company_id", 38L);
		pr.put("user_id", 3847L);
		pr.put("mobile", "13800000000");
		pr.put("distributor_id", 310L);
		pr.put("order_type", "normal_shopadmin");
		pr.put("receipt_type", "ziti");
		pr.put("order_source", "shop_offline");
		pr.put("pay_type", "deposit");
		pr.put("is_online_order", Boolean.FALSE);
		Map<String, Object> cartLine = new LinkedHashMap<>();
		cartLine.put("item_id", 11L);
		cartLine.put("price", 100);
		cartLine.put("item_name", "sku");
		cartLine.put("total_fee", 10000);
		pr.put("_checkout_cart_meta", Map.of("list", List.of(cartLine)));
		pr.put("items", List.of(Map.of("item_id", 11L, "num", 1)));
		return p;
	}

	@Test
	void format_doesNotThrow_whenDepositBalanceInsufficient() {
		NormalOrderCreateParams p = depositParams();

		assertThatCode(() -> port.format(p)).doesNotThrowAnyException();

		Map<String, Object> od = p.getOrderData();
		assertThat(((Number) od.get("total_fee")).longValue()).isPositive();
		verifyNoInteractions(userDepositBalanceReadService);
	}

	@Test
	void assertDepositBalance_throwsInsufficientBalance_whenBalanceShort() {
		when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
				.thenReturn("余额不足");
		when(userDepositBalanceReadService.getUserDepositTotal(38L, 3847L)).thenReturn(0L);
		NormalOrderCreateParams p = depositParams();
		port.format(p);

		long needPay = ((Number) p.getOrderData().get("total_fee")).longValue();
		assertThat(needPay).isPositive();

		assertThatThrownBy(() -> port.assertDepositBalance(p))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("余额不足");
	}

	@Test
	void assertDepositBalance_passes_whenBalanceCoversNeedPay() {
		when(userDepositBalanceReadService.getUserDepositTotal(38L, 3847L)).thenReturn(999999L);
		NormalOrderCreateParams p = depositParams();
		port.format(p);

		assertThatCode(() -> port.assertDepositBalance(p)).doesNotThrowAnyException();
	}

	@Test
	void assertDepositBalance_skipsNonDepositPayType_withoutReadingBalance() {
		NormalOrderCreateParams p = depositParams();
		p.getParams().put("pay_type", "pos");
		port.format(p);

		assertThatCode(() -> port.assertDepositBalance(p)).doesNotThrowAnyException();
		verifyNoInteractions(userDepositBalanceReadService);
	}
}
