package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.cart.WxappUserCartListForCouponCheckoutPort;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartAdminSubmitDataService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDiscountValidItemsResolverTest {

	@Mock
	private WxappUserCartListForCouponCheckoutPort wxappUserCartListForCouponCheckoutPort;

	@Mock
	private OperatorCartAdminSubmitDataService operatorCartAdminSubmitDataService;

	private UserDiscountValidItemsResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new UserDiscountValidItemsResolver(
				wxappUserCartListForCouponCheckoutPort,
				operatorCartAdminSubmitDataService,
				new ObjectMapper());
	}

	@Test
	@DisplayName("结算传入 items 时只按结算商品匹配，不把购物车里未结算的活动商品算进来")
	void checkoutUsesPayloadItemsNotFullCart() {
		UserDiscountNewGetCardListRequest req = new UserDiscountNewGetCardListRequest(
				null,
				"[{\"item_id\":2446,\"num\":5,\"price\":30}]",
				null,
				null,
				null,
				null,
				1,
				10,
				"mall",
				"picker",
				null,
				"0",
				"true",
				"cart",
				null,
				null,
				"0",
				null,
				"true");

		Map<Long, Map<String, Object>> items = resolver.resolve(1L, 45097L, req);

		assertThat(items).containsOnlyKeys(2446L);
		assertThat(items).doesNotContainKey(2614L);
		assertThat(((Number) items.get(2446L).get("total_fee")).longValue()).isEqualTo(15000L);
		verify(wxappUserCartListForCouponCheckoutPort, never())
				.getCartList(anyLong(), anyLong(), anyLong(), anyString(), anyInt(), anyInt());
	}
}
