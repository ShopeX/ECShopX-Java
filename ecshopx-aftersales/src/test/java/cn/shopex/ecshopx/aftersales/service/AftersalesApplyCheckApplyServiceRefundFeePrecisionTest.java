package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("售后申请退款金额校验：分单位整数比较")
class AftersalesApplyCheckApplyServiceRefundFeePrecisionTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 5365437000056517L;
	private static final long SUB_ORDER_ID = 47298L;

	@Test
	@DisplayName("请求退款金额含浮点精度噪声时，应按分取整后与可退金额比较并通过")
	void checkApply_acceptsFloatingPointNoiseEqualToRefundableFen() {
		AftersalesApplyCheckApplyService service = newService(440);

		AftersalesApplyParams params = baseParams();
		params.setRefundFeeRaw("440.00000000000006");

		assertThatCode(() -> service.checkApply(params)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("请求退款金额取整后仍超过可退金额时应拒绝")
	void checkApply_rejectsWhenRoundedRequestExceedsRefundableFen() {
		AftersalesApplyCheckApplyService service = newService(440);

		AftersalesApplyParams params = baseParams();
		params.setRefundFeeRaw("441");

		assertThatThrownBy(() -> service.checkApply(params))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("退款金额不能超过可退金额");
	}

	private static AftersalesApplyCheckApplyService newService(int lineTotalFeeFen) {
		OrderNormalOrderHeaderReadPort headerPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderNormalOrderItemsReadPort itemsPort = mock(OrderNormalOrderItemsReadPort.class);
		AftersalesApplyDetailQueryService detailQuery = mock(AftersalesApplyDetailQueryService.class);

		when(headerPort.getHeader(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(Optional.of(orderHeader()));
		when(itemsPort.listItems(eq(COMPANY_ID), eq(ORDER_ID)))
				.thenReturn(List.of(orderLine(lineTotalFeeFen)));
		when(detailQuery.sumAppliedNum(anyLong(), anyLong(), anyLong())).thenReturn(0);
		when(detailQuery.sumAppliedRefundFee(anyLong(), anyLong(), anyLong())).thenReturn(0);
		when(detailQuery.sumAppliedRefundPoint(anyLong(), anyLong(), anyLong())).thenReturn(0);

		return new AftersalesApplyCheckApplyService(headerPort, itemsPort, detailQuery);
	}

	private static AftersalesApplyParams baseParams() {
		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setCompanyId(COMPANY_ID);
		params.setOrderId(ORDER_ID);
		params.setAftersalesType("ONLY_REFUND");
		params.setReason("收到残次品");
		params.setRefundPointRaw("0");
		params.setFreight(0);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", SUB_ORDER_ID);
		row.put("num", 1);
		params.setDetailRows(List.of(row));
		params.syncDetailLinesFromRows();
		return params;
	}

	private static Map<String, Object> orderHeader() {
		Map<String, Object> h = new LinkedHashMap<>();
		h.put("order_status", "PAID");
		h.put("receipt_type", "logistics");
		h.put("delivery_status", "DONE");
		h.put("order_auto_close_aftersales_time", 0);
		h.put("freight_type", "cash");
		h.put("freight_fee", 0);
		return h;
	}

	private static Map<String, Object> orderLine(int totalFeeFen) {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", SUB_ORDER_ID);
		line.put("num", 1);
		line.put("item_name", "测试商品");
		line.put("delivery_status", "DONE");
		line.put("delivery_item_num", 1);
		line.put("cancel_item_num", 0);
		line.put("total_fee", totalFeeFen);
		line.put("point", 0);
		line.put("order_item_type", "normal");
		return line;
	}
}
