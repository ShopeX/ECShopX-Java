package cn.shopex.ecshopx.orders.service.normal.create;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NormalOrderPointDeductFormatterPayTypePointTest {

	private PointMemberRuleReadService ruleReadService;
	private PointMemberBalanceReadService balanceReadService;
	private NormalOrderCheckoutPointDeductService checkoutPointDeductService;
	private NormalOrderPointDeductFormatter formatter;

	@BeforeEach
	void setUp() {
		ruleReadService = mock(PointMemberRuleReadService.class);
		balanceReadService = mock(PointMemberBalanceReadService.class);
		checkoutPointDeductService = mock(NormalOrderCheckoutPointDeductService.class);
		formatter =
				new NormalOrderPointDeductFormatter(
						ruleReadService, balanceReadService, checkoutPointDeductService);
		when(ruleReadService.getIsOpenPoint(anyLong())).thenReturn(true);
		when(ruleReadService.getPointRule(anyLong())).thenReturn(Map.of("name", "积分"));
		when(balanceReadService.getPointBalance(anyLong(), anyLong())).thenReturn(99999L);
	}

	@Test
	@DisplayName("pay_type=point 时仍走积分抵扣（对齐 PHP，否则 total_fee 不归零无法自动支付）")
	void applyIfNeeded_whenPayTypePoint_stillAppliesCheckoutDeduct() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 1L);
		od.put("user_id", 45097L);
		od.put("pay_type", "point");
		od.put("point_use", 14000);
		od.put("total_fee", 14000L);
		p.setOrderData(od);
		p.getParams().put("pay_type", "point");
		p.getParams().put("point_use", 14000);

		formatter.applyIfNeeded(p);

		verify(checkoutPointDeductService)
				.applyCheckoutPointDeduct(
						eq(od), eq(p.getParams()), eq(1L), eq(45097L), any(), eq(99999L));
	}

	@Test
	@DisplayName("积分商城 pay_type=point：跳过普通积分抵扣（按商品积分价支付）")
	void applyIfNeeded_whenPointsmallPayTypePoint_skipsCheckoutDeduct() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 38L);
		od.put("user_id", 1170L);
		od.put("order_class", "pointsmall");
		od.put("pay_type", "point");
		od.put("point_use", 10000);
		od.put("point", 10000L);
		od.put("total_fee", 999900L);
		p.setOrderData(od);
		p.getParams().put("order_type", "normal_pointsmall");
		p.getParams().put("pay_type", "point");
		p.getParams().put("point_use", 10000);

		formatter.applyIfNeeded(p);

		verify(checkoutPointDeductService, never())
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
	}

	@Test
	@DisplayName("无积分且非 point 支付：不调用抵扣")
	void applyIfNeeded_whenNoPointUseAndNotPointPay_skips() {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 1L);
		od.put("user_id", 1L);
		od.put("pay_type", "wxpay");
		od.put("point_use", 0);
		p.setOrderData(od);
		p.getParams().put("pay_type", "wxpay");

		formatter.applyIfNeeded(p);

		verify(checkoutPointDeductService, never())
				.applyCheckoutPointDeduct(any(), any(), anyLong(), anyLong(), any(), anyLong());
	}
}
