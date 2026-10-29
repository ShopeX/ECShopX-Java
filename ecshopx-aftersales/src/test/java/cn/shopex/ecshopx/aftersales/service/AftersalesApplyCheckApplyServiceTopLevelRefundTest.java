package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
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
@DisplayName("售后申请：顶层退款金额/积分不参与拆单校验")
class AftersalesApplyCheckApplyServiceTopLevelRefundTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 5365437000056517L;
	private static final long SUB_ORDER_ID = 47298L;

	@Test
	@DisplayName("顶层金额积分超过订单行金额积分时仍通过，由 handle 层按单件重算")
	void checkApply_ignoresTopLevelRefundFeeAndPoint() {
		AftersalesApplyCheckApplyService service = newService(440);

		AftersalesApplyParams params = baseParams();
		params.setRefundFeeRaw("441");
		params.setRefundPointRaw("999");

		assertThatCode(() -> service.checkApply(params)).doesNotThrowAnyException();
	}

	private static AftersalesApplyCheckApplyService newService(int lineTotalFeeFen) {
		OrderNormalOrderHeaderReadPort headerPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderNormalOrderItemsReadPort itemsPort = mock(OrderNormalOrderItemsReadPort.class);
		AftersalesApplyDetailQueryService detailQuery = mock(AftersalesApplyDetailQueryService.class);

		when(headerPort.getHeader(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(Optional.of(orderHeader()));
		when(itemsPort.listItems(eq(COMPANY_ID), eq(ORDER_ID)))
				.thenReturn(List.of(orderLine(lineTotalFeeFen)));

		return new AftersalesApplyCheckApplyService(
				headerPort, itemsPort, detailQuery, AftersalesTestMessageSource.FALLBACK);
	}

	private static AftersalesApplyParams baseParams() {
		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setCompanyId(COMPANY_ID);
		params.setOrderId(ORDER_ID);
		params.setAftersalesType("REFUND_GOODS");
		params.setReason("收到残次品");
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
