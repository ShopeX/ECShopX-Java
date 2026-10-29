package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.common.exception.ResourceException;
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
@DisplayName("售后申请数量校验：对齐 PHP")
class AftersalesApplyCheckApplyServiceQuantityTest {

	private static final long COMPANY_ID = 141L;
	private static final long ORDER_ID = 5406561000261112L;
	private static final long SUB_ORDER_ID = 5996L;

	private OrderNormalOrderHeaderReadPort headerPort;
	private OrderNormalOrderItemsReadPort itemsPort;
	private AftersalesApplyDetailQueryService detailQuery;
	private StaticMessageSource messageSource;
	private AftersalesApplyCheckApplyService service;

	@BeforeEach
	void setUp() {
		headerPort = mock(OrderNormalOrderHeaderReadPort.class);
		itemsPort = mock(OrderNormalOrderItemsReadPort.class);
		detailQuery = mock(AftersalesApplyDetailQueryService.class);
		messageSource = new StaticMessageSource();
		messageSource.addMessage(
				"aftersales.apply.reason_required", Locale.SIMPLIFIED_CHINESE, "售后理由必选");
		messageSource.addMessage(
				"aftersales.apply.quantity_exceeded",
				Locale.SIMPLIFIED_CHINESE,
				"{0} 剩余可申请售后的数量为{1}");
		messageSource.addMessage(
				"aftersales.apply.reason_required", Locale.US, "After-sales reason is required.");
		messageSource.addMessage(
				"aftersales.apply.quantity_exceeded",
				Locale.US,
				"{0}: only {1} item(s) remain available for after-sales application.");
		stubHeader("PAID", "PARTAIL", "logistics");
		stubLine(3, 1, 0, "PENDING");
		service = new AftersalesApplyCheckApplyService(headerPort, itemsPort, detailQuery, messageSource);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	@DisplayName("消费者仅退款按未发货数量校验，部分发货可退剩余2件")
	void onlyRefund_partialDelivery_allowsUnshippedQuantity() {
		assertThatCode(() -> service.checkApply(params("ONLY_REFUND", 2, "196")))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("消费者仅退款超过未发货数量时拒绝")
	void onlyRefund_rejectsQuantityBeyondUnshipped() {
		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 3, "294")))
				.isInstanceOf(ResourceException.class)
				.hasMessage("供应商商品A 剩余可申请售后的数量为2");
	}

	@Test
	@DisplayName("英文环境返回英文数量校验文案")
	void quantityExceeded_usesEnglishMessage() {
		LocaleContextHolder.setLocale(Locale.US, true);

		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 3, "294")))
				.isInstanceOf(ResourceException.class)
				.hasMessage("供应商商品A: only 2 item(s) remain available for after-sales application.");
	}

	@Test
	@DisplayName("售后理由为空时使用 PHP 服务层文案")
	void emptyReason_usesPhpServiceWording() {
		AftersalesApplyParams params = params("ONLY_REFUND", 1, "98");
		params.setReason("");

		assertThatThrownBy(() -> service.checkApply(params))
				.isInstanceOf(ResourceException.class)
				.hasMessage("售后理由必选");
	}

	@Test
	@DisplayName("消费者仅退款扣减已申请仅退款数量")
	void onlyRefund_deductsAppliedOnlyRefundQuantity() {
		when(detailQuery.sumAppliedNumByType(anyLong(), anyLong(), anyLong(), eq("ONLY_REFUND")))
				.thenReturn(1);

		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 2, "196")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("剩余可申请售后的数量为1");
	}

	@Test
	@DisplayName("消费者仅退款扣减取消数量")
	void onlyRefund_deductsCancelledQuantity() {
		stubLine(3, 1, 1, "PENDING");

		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 2, "196")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("剩余可申请售后的数量为1");
	}

	@Test
	@DisplayName("退换货不占用仅退款已申请数量")
	void returnExchange_ignoresAppliedOnlyRefundQuantity() {
		lenient()
				.when(detailQuery.sumAppliedNumByType(anyLong(), anyLong(), anyLong(), eq("ONLY_REFUND")))
				.thenReturn(1);
		when(detailQuery.sumAppliedNumByType(
						anyLong(), anyLong(), anyLong(), eq("REFUND_GOODS"), eq("EXCHANGING_GOODS")))
				.thenReturn(0);

		assertThatCode(() -> service.checkApply(params("REFUND_GOODS", 1, "98")))
				.doesNotThrowAnyException();
		verify(detailQuery, never())
				.sumAppliedNumByType(anyLong(), anyLong(), anyLong(), eq("ONLY_REFUND"));
	}

	@Test
	@DisplayName("退换货扣减已申请退货退款和换货数量")
	void returnExchange_deductsAppliedReturnExchangeQuantity() {
		when(detailQuery.sumAppliedNumByType(
						anyLong(), anyLong(), anyLong(), eq("REFUND_GOODS"), eq("EXCHANGING_GOODS")))
				.thenReturn(1);

		assertThatThrownBy(() -> service.checkApply(params("REFUND_GOODS", 1, "98")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("剩余可申请售后的数量为0");
	}

	@Test
	@DisplayName("退换货发货数量按子订单数量封顶")
	void returnExchange_capsDeliveryAtLineNum() {
		stubLine(3, 5, 0, "DONE");
		when(detailQuery.sumAppliedNumByType(
						anyLong(), anyLong(), anyLong(), eq("REFUND_GOODS"), eq("EXCHANGING_GOODS")))
				.thenReturn(2);

		assertThatThrownBy(() -> service.checkApply(params("REFUND_GOODS", 2, "196")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("剩余可申请售后的数量为1");
	}

	@Test
	@DisplayName("后台整单已发货仅退款按已发货数量校验")
	void adminFullyDeliveredOnlyRefund_allowsDeliveredQuantity() {
		stubHeader("PAID", "DONE", "logistics");
		stubLine(3, 3, 0, "DONE");

		assertThatCode(() -> service.checkApply(params("ONLY_REFUND", 3, "294"), true))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("消费者整单已发货仅退款拒绝")
	void frontFullyDeliveredOnlyRefund_rejects() {
		stubHeader("PAID", "DONE", "logistics");
		stubLine(3, 3, 0, "DONE");

		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 1, "98")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("剩余可申请售后的数量为0");
	}

	@Test
	@DisplayName("后台未整单发货仍按未发货数量校验")
	void adminPartialDeliveryOnlyRefund_usesUnshippedQuantity() {
		assertThatCode(() -> service.checkApply(params("ONLY_REFUND", 2, "196"), true))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("自提订单未核销不能申请售后")
	void zitiNotCheckedIn_rejects() {
		stubHeader("PAID", "NOTZITI", "ziti");

		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 2, "196")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("请先核销订单");
	}

	@Test
	@DisplayName("未支付订单不能申请售后")
	void notPaidOrder_rejects() {
		stubHeader("NOTPAY", "PARTAIL", "logistics");

		assertThatThrownBy(() -> service.checkApply(params("ONLY_REFUND", 2, "196")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("该订单不能申请售后");
	}

	private void stubHeader(String orderStatus, String deliveryStatus, String receiptType) {
		when(headerPort.getHeader(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(header(orderStatus, deliveryStatus, receiptType)));
	}

	private void stubLine(int num, int deliveryItemNum, int cancelItemNum, String deliveryStatus) {
		lenient()
				.when(itemsPort.listItems(COMPANY_ID, ORDER_ID))
				.thenReturn(List.of(line(num, deliveryItemNum, cancelItemNum, deliveryStatus)));
	}

	private static AftersalesApplyParams params(String aftersalesType, int num, String refundFee) {
		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setCompanyId(COMPANY_ID);
		params.setOrderId(ORDER_ID);
		params.setAftersalesType(aftersalesType);
		params.setReason("测试售后");
		params.setRefundFeeRaw(refundFee);
		params.setRefundPointRaw("0");
		params.setFreight(0);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", SUB_ORDER_ID);
		row.put("num", num);
		params.setDetailRows(List.of(row));
		params.syncDetailLinesFromRows();
		return params;
	}

	private static Map<String, Object> header(
			String orderStatus, String deliveryStatus, String receiptType) {
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("order_status", orderStatus);
		header.put("delivery_status", deliveryStatus);
		header.put("receipt_type", receiptType);
		header.put("order_auto_close_aftersales_time", 0);
		header.put("freight_type", "cash");
		header.put("freight_fee", 0);
		return header;
	}

	private static Map<String, Object> line(
			int num, int deliveryItemNum, int cancelItemNum, String deliveryStatus) {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", SUB_ORDER_ID);
		line.put("num", num);
		line.put("delivery_item_num", deliveryItemNum);
		line.put("delivery_status", deliveryStatus);
		line.put("cancel_item_num", cancelItemNum);
		line.put("total_fee", 294);
		line.put("point", 0);
		line.put("item_name", "供应商商品A");
		line.put("order_item_type", "normal");
		return line;
	}
}
