package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("售后申请运费校验：对齐 PHP")
class AftersalesApplyCheckApplyServiceFreightTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 100L;
	private static final long SUB_ORDER_ID = 501L;

	private OrderNormalOrderHeaderReadPort headerPort;
	private OrderNormalOrderItemsReadPort itemsPort;
	private AftersalesApplyDetailQueryService detailQuery;

	@BeforeEach
	void setUp() {
		headerPort = mock(OrderNormalOrderHeaderReadPort.class);
		itemsPort = mock(OrderNormalOrderItemsReadPort.class);
		detailQuery = mock(AftersalesApplyDetailQueryService.class);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	@DisplayName("仅退款带运费时不进入运费校验")
	void onlyRefund_withFreight_skipsFreightValidation() {
		stubOrder("cash", 1_000);
		AftersalesApplyCheckApplyService service = service();

		assertThatCode(() -> service.checkApply(params("ONLY_REFUND", 1_000), true))
				.doesNotThrowAnyException();
		verify(detailQuery, never()).sumAppliedFreightCash(anyLong(), anyLong());
	}

	@Test
	@DisplayName("积分运费按 freight_fee 校验，不按 freight_point")
	void pointFreight_usesFreightFee() {
		stubOrder("point", 300);
		when(detailQuery.sumAppliedFreightPoint(COMPANY_ID, ORDER_ID)).thenReturn(0);
		AftersalesApplyCheckApplyService service = service();

		assertThatCode(() -> service.checkApply(params("REFUND_GOODS", 200)))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("积分运费超出剩余积分时拒绝")
	void pointFreight_exceedingRemaining_rejects() {
		stubOrder("point", 100);
		when(detailQuery.sumAppliedFreightPoint(COMPANY_ID, ORDER_ID)).thenReturn(0);
		AftersalesApplyCheckApplyService service = service();

		assertThatThrownBy(() -> service.checkApply(params("REFUND_GOODS", 200)))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("申请退款积分运费不能超出支付积分运费");
	}

	@Test
	@DisplayName("积分运费未支付时返回英文文案")
	void pointFreightNotPaid_usesEnglishMessage() {
		StaticMessageSource messages = new StaticMessageSource();
		messages.addMessage(
				"aftersales.apply.freight_point_not_paid",
				Locale.US,
				"The order has no paid point freight.");
		LocaleContextHolder.setLocale(Locale.US, true);
		stubOrder("point", 0);
		AftersalesApplyCheckApplyService service =
				new AftersalesApplyCheckApplyService(headerPort, itemsPort, detailQuery, messages);

		assertThatThrownBy(() -> service.checkApply(params("REFUND_GOODS", 100)))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("The order has no paid point freight.");
	}

	private AftersalesApplyCheckApplyService service() {
		return new AftersalesApplyCheckApplyService(
				headerPort, itemsPort, detailQuery, AftersalesTestMessageSource.FALLBACK);
	}

	private void stubOrder(String freightType, int freightFee) {
		when(headerPort.getHeader(COMPANY_ID, ORDER_ID)).thenReturn(Optional.of(header(freightType, freightFee)));
		when(itemsPort.listItems(COMPANY_ID, ORDER_ID)).thenReturn(List.of(orderLine()));
	}

	private static Map<String, Object> header(String freightType, int freightFee) {
		Map<String, Object> h = new LinkedHashMap<>();
		h.put("order_status", "PAID");
		h.put("receipt_type", "logistics");
		h.put("delivery_status", "DONE");
		h.put("order_auto_close_aftersales_time", 0);
		h.put("freight_type", freightType);
		h.put("freight_fee", freightFee);
		h.put("freight_point", 0);
		return h;
	}

	private static Map<String, Object> orderLine() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", SUB_ORDER_ID);
		line.put("num", 1);
		line.put("item_name", "测试商品");
		line.put("delivery_status", "DONE");
		line.put("delivery_item_num", 1);
		line.put("cancel_item_num", 0);
		line.put("total_fee", 1_000);
		line.put("point", 0);
		line.put("order_item_type", "product");
		return line;
	}

	private static AftersalesApplyParams params(String aftersalesType, int freight) {
		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setCompanyId(COMPANY_ID);
		params.setOrderId(ORDER_ID);
		params.setAftersalesType(aftersalesType);
		params.setReason("收到残次品");
		params.setFreight(freight);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", SUB_ORDER_ID);
		row.put("num", 1);
		params.setDetailRows(List.of(row));
		params.syncDetailLinesFromRows();
		return params;
	}
}
