package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("售后按数量拆单：每件使用原始金额积分计算")
class AftersalesApplyShopApplyByNumServiceSplitTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 2L;
	private static final long SUB_ORDER_ID = 10L;

	@Test
	@DisplayName("num=3 时忽略明细 total_fee/total_point，由订单行金额积分分摊")
	void splitIgnoresDetailTotalFeeAndPoint() {
		AftersalesApplyCheckApplyService checkService =
				mock(AftersalesApplyCheckApplyService.class);
		AftersalesApplyShopApplyByNumHandleService handleService =
				mock(AftersalesApplyShopApplyByNumHandleService.class);
		OrderNormalOrderHeaderReadPort headerPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderSuccessTradeReadPort tradePort = mock(OrderSuccessTradeReadPort.class);

		when(headerPort.getHeader(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(new LinkedHashMap<>(Map.of("order_id", ORDER_ID))));
		when(tradePort.primarySuccessTrade(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(new LinkedHashMap<>(Map.of("fee_type", "CNY"))));

		AftersalesApplyShopApplyByNumService service =
				new AftersalesApplyShopApplyByNumService(checkService, handleService, headerPort, tradePort);

		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setCompanyId(COMPANY_ID);
		params.setOrderId(ORDER_ID);
		params.setAftersalesType("REFUND_GOODS");
		params.setReason("收到残次品");
		params.setRefundFeeRaw("300");
		params.setRefundPointRaw("600");

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", SUB_ORDER_ID);
		row.put("num", 3);
		row.put("total_fee", 300);
		row.put("total_point", 600);
		params.setDetailRows(List.of(row));
		params.syncDetailLinesFromRows();

		service.shopApplyByNum(params);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
		verify(handleService, times(3))
				.shopApplyByNumHandle(any(), any(), dataCaptor.capture());

		for (Map<String, Object> data : dataCaptor.getAllValues()) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> detail = (List<Map<String, Object>>) data.get("detail");
			assertThat(detail).hasSize(1);
			assertThat(detail.get(0).get("num")).isEqualTo(1);
			assertThat(detail.get(0)).doesNotContainKeys("total_fee", "total_point");
		}
	}
}
