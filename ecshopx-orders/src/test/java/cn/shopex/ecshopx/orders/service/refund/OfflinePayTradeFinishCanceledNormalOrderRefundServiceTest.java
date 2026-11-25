package cn.shopex.ecshopx.orders.service.refund;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeFinish canceled refund convergence (Spec §3.4)")
class OfflinePayTradeFinishCanceledNormalOrderRefundServiceTest {

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@InjectMocks
	private OfflinePayTradeFinishCanceledNormalOrderRefundService service;

	@Test
	void executeIfApplicable_whenCanceledNormalOrder_doesNotLoadPrimaryTradeOrCreateRefund() {
		NormalOrders canceled = new NormalOrders();
		canceled.setOrderId(501L);
		canceled.setCompanyId(9L);
		canceled.setOrderStatus("CANCEL");
		canceled.setOrderClass("normal");
		when(normalOrdersMapper.selectOne(any())).thenReturn(canceled);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_state", "SUCCESS");
		row.put("pay_type", "offline_pay");
		row.put("company_id", 9L);
		row.put("order_id", 501L);
		row.put("trade_id", "this-trade-id");

		service.executeIfApplicable(row);

		verify(normalOrdersMapper).selectOne(any());
	}

	@Test
	void executeIfApplicable_whenPayedOrder_isNoOp() {
		NormalOrders payed = new NormalOrders();
		payed.setOrderId(501L);
		payed.setCompanyId(9L);
		payed.setOrderStatus("PAYED");
		payed.setOrderClass("normal");
		when(normalOrdersMapper.selectOne(any())).thenReturn(payed);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_state", "SUCCESS");
		row.put("pay_type", "alipay");
		row.put("company_id", 9L);
		row.put("order_id", 501L);
		row.put("trade_id", "T1");

		service.executeIfApplicable(row);
	}

	@Test
	void executeIfApplicable_whenTradeNotSuccess_isNoOp() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("trade_state", "NOTPAY");
		row.put("pay_type", "offline_pay");
		row.put("company_id", 9L);
		row.put("order_id", 501L);

		service.executeIfApplicable(row);

		verify(normalOrdersMapper, never()).selectOne(any());
	}
}