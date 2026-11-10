package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartPointPreviewFacade;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * POS 购物车积分抵扣预览（ECX-11082 后续）：组装券后 od 调用下单同口径的
 * {@link NormalOrderCheckoutPointDeductService}，并把积分字段白名单合并回 shop 桶；
 * total_fee 再减 point_fee。仅预览，不影响下单链路。
 */
@ExtendWith(MockitoExtension.class)
class OperatorCartPointPreviewFacadeImplTest {

	private static final long COMPANY_ID = 38L;
	private static final long USER_ID = 3850L;
	private static final long SHOP_ID = 129L;

	@Mock private PointMemberRuleReadService pointMemberRuleReadService;
	@Mock private PointMemberBalanceReadService pointMemberBalanceReadService;
	@Mock private NormalOrderCheckoutPointDeductService checkoutPointDeductService;

	private OperatorCartPointPreviewFacadeImpl facade;

	@BeforeEach
	void setUp() {
		facade = new OperatorCartPointPreviewFacadeImpl(
				pointMemberRuleReadService, pointMemberBalanceReadService, checkoutPointDeductService);
	}

	@Test
	@DisplayName("只预览能力：od 带券后 total_fee 与桶行引用，合并余额/上限/规则，total_fee 不变")
	void applyPointPreview_withoutPointUseMergesCapabilityFields() {
		Map<String, Object> cartData = cartData(15120L);
		doAnswer(inv -> {
			Map<String, Object> od = inv.getArgument(0);
			od.put("user_point", "20000");
			od.put("max_point", 15120);
			od.put("limit_point", 4536);
			od.put("deduct_point_rule", Map.of("isOpenMemberPoint", true, "isOpenDeductPoint", true));
			od.put("is_open_deduct_point", true);
			return null;
		}).when(checkoutPointDeductService)
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
		when(pointMemberRuleReadService.getPointRule(COMPANY_ID)).thenReturn(Map.of());
		when(pointMemberBalanceReadService.getPointBalance(COMPANY_ID, USER_ID)).thenReturn(20000L);

		facade.applyPointPreview(COMPANY_ID, USER_ID, cartData, null);

		ArgumentCaptor<Map<String, Object>> odCaptor = mapCaptor();
		ArgumentCaptor<Map<String, Object>> paramsCaptor = mapCaptor();
		verify(checkoutPointDeductService).applyCheckoutPointDeduct(
				odCaptor.capture(), paramsCaptor.capture(), eq(COMPANY_ID), eq(USER_ID), any(), anyLong());
		Map<String, Object> od = odCaptor.getValue();
		assertThat(((Number) od.get("total_fee")).longValue()).isEqualTo(15120L);
		assertThat(od.get("items")).isSameAs(shopOf(cartData).get("list"));
		assertThat(od.get("point_use")).isNull();

		Map<String, Object> shop = shopOf(cartData);
		assertThat(String.valueOf(shop.get("user_point"))).isEqualTo("20000");
		assertThat(((Number) shop.get("max_point")).intValue()).isEqualTo(15120);
		assertThat(Boolean.TRUE.equals(shop.get("is_open_deduct_point"))).isTrue();
		assertThat(shop.get("point_fee")).isNull();
		assertThat(((Number) shop.get("total_fee")).longValue()).isEqualTo(15120L);
	}

	@Test
	@DisplayName("输入积分：od.point_use=N，point_fee 抵扣回写并减 total_fee")
	void applyPointPreview_withPointUseDeductsTotalFee() {
		Map<String, Object> cartData = cartData(15120L);
		doAnswer(inv -> {
			Map<String, Object> od = inv.getArgument(0);
			od.put("user_point", "20000");
			od.put("max_point", 15120);
			od.put("is_open_deduct_point", true);
			od.put("point_use", 1000);
			od.put("real_use_point", 1000);
			od.put("point_fee", 100L);
			od.put("total_fee", 15020L);
			return null;
		}).when(checkoutPointDeductService)
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
		when(pointMemberRuleReadService.getPointRule(COMPANY_ID)).thenReturn(Map.of());
		when(pointMemberBalanceReadService.getPointBalance(COMPANY_ID, USER_ID)).thenReturn(20000L);

		facade.applyPointPreview(COMPANY_ID, USER_ID, cartData, 1000);

		Map<String, Object> shop = shopOf(cartData);
		assertThat(((Number) shop.get("point_fee")).longValue()).isEqualTo(100L);
		assertThat(((Number) shop.get("total_fee")).longValue()).isEqualTo(15020L);
		assertThat(((Number) shop.get("point_use")).intValue()).isEqualTo(1000);
	}

	@Test
	@DisplayName("开关关闭：合并关闭状态与 0 上限，不改 total_fee")
	void applyPointPreview_closedRuleKeepsTotalsUntouched() {
		Map<String, Object> cartData = cartData(15120L);
		doAnswer(inv -> {
			Map<String, Object> od = inv.getArgument(0);
			od.put("user_point", "20000");
			od.put("max_point", 0);
			od.put("is_open_deduct_point", false);
			od.put("deduct_point_rule", Map.of("isOpenDeductPoint", false));
			return null;
		}).when(checkoutPointDeductService)
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
		when(pointMemberRuleReadService.getPointRule(COMPANY_ID)).thenReturn(Map.of());
		when(pointMemberBalanceReadService.getPointBalance(COMPANY_ID, USER_ID)).thenReturn(20000L);

		facade.applyPointPreview(COMPANY_ID, USER_ID, cartData, null);

		Map<String, Object> shop = shopOf(cartData);
		assertThat(Boolean.FALSE.equals(shop.get("is_open_deduct_point"))).isTrue();
		assertThat(((Number) shop.get("max_point")).intValue()).isEqualTo(0);
		assertThat(((Number) shop.get("total_fee")).longValue()).isEqualTo(15120L);
	}

	@Test
	@DisplayName("积分不足等校验异常向上抛出（收银员需要输入反馈）")
	void applyPointPreview_propagatesValidationException() {
		Map<String, Object> cartData = cartData(15120L);
		doThrow(new ResourceException("积分不足"))
				.when(checkoutPointDeductService)
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
		when(pointMemberRuleReadService.getPointRule(COMPANY_ID)).thenReturn(Map.of());
		when(pointMemberBalanceReadService.getPointBalance(COMPANY_ID, USER_ID)).thenReturn(10L);

		assertThatThrownBy(() -> facade.applyPointPreview(COMPANY_ID, USER_ID, cartData, 999999))
				.isInstanceOf(ResourceException.class);
	}

	@Test
	@DisplayName("无会员/空购物车不触发积分计算")
	void applyPointPreview_skipsForNoMemberOrEmptyCart() {
		Map<String, Object> cartData = cartData(15120L);

		facade.applyPointPreview(COMPANY_ID, 0L, cartData, 1000);
		Map<String, Object> emptyCart = new LinkedHashMap<>();
		emptyCart.put("valid_cart", new ArrayList<>());
		facade.applyPointPreview(COMPANY_ID, USER_ID, emptyCart, 1000);

		verify(checkoutPointDeductService, never())
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> cartData(long totalFee) {
		Map<String, Object> lineA = new LinkedHashMap<>();
		lineA.put("cart_id", 631L);
		lineA.put("item_id", 7666L);
		lineA.put("num", 1);
		lineA.put("total_fee", 15120);
		Map<String, Object> shop = new LinkedHashMap<>();
		shop.put("shop_id", SHOP_ID);
		shop.put("discount_fee", 4780L);
		shop.put("total_fee", totalFee);
		shop.put("list", new ArrayList<>(List.of(lineA)));
		Map<String, Object> cartData = new LinkedHashMap<>();
		cartData.put("valid_cart", new ArrayList<>(List.of(shop)));
		return cartData;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> shopOf(Map<String, Object> cartData) {
		return ((List<Map<String, Object>>) cartData.get("valid_cart")).get(0);
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Map<String, Object>> mapCaptor() {
		return ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
	}
}
