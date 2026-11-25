package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundServiceCreateAbnormalTradeRefundTest {

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher;

	private AftersalesRefundService service;

	@BeforeEach
	void init() {
		service = new AftersalesRefundService(aftersalesRefundMapper, aftersalesRefundJobDispatchPublisher);
	}

	@Test
	@DisplayName("§2.2 逾期异常：字段来自本笔 trade，refund_status=PAY_AFTER_CANCEL")
	void createAbnormalTradeRefund_payAfterCancel_persistsSpecFields() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(null);
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class))).thenReturn(1);

		Map<String, Object> order = baseOrder();
		Map<String, Object> tradeRow = wxTradeRow("T-WX-001", 12_500);

		AftersalesRefund created =
				service.createAbnormalTradeRefund(
						order, tradeRow, AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);

		ArgumentCaptor<AftersalesRefund> captor = ArgumentCaptor.forClass(AftersalesRefund.class);
		verify(aftersalesRefundMapper, times(1)).insert(captor.capture());
		AftersalesRefund row = captor.getValue();
		assertThat(row.getRefundType()).isEqualTo("1");
		assertThat(row.getRefundStatus()).isEqualTo("PAY_AFTER_CANCEL");
		assertThat(row.getTradeId()).isEqualTo("T-WX-001");
		assertThat(row.getRefundFee()).isEqualTo(12_500);
		assertThat(row.getReturnFreight()).isEqualTo(0);
		assertThat(row.getRefundChannel()).isEqualTo("original");
		assertThat(row.getPayType()).isEqualTo("wxpay");
		assertThat(row.getRefundStatus()).isNotIn("READY", "AUDIT_SUCCESS");
		assertThat(created.getRefundStatus()).isEqualTo("PAY_AFTER_CANCEL");
	}

	@Test
	@DisplayName("§2.2 重复支付：线下 trade 走 offline 渠道，status=DUP_PAY")
	void createAbnormalTradeRefund_dupPay_offlineChannelFromTradePayType() {
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(null);
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class))).thenReturn(1);

		Map<String, Object> order = baseOrder();
		Map<String, Object> tradeRow = baseTradeRow("T-OFF-9", "offline_pay", 88_800);

		AftersalesRefund created =
				service.createAbnormalTradeRefund(order, tradeRow, AftersalesRefundService.REFUND_STATUS_DUP_PAY);

		ArgumentCaptor<AftersalesRefund> captor = ArgumentCaptor.forClass(AftersalesRefund.class);
		verify(aftersalesRefundMapper).insert(captor.capture());
		AftersalesRefund row = captor.getValue();
		assertThat(row.getRefundStatus()).isEqualTo("DUP_PAY");
		assertThat(row.getRefundChannel()).isEqualTo("offline");
		assertThat(row.getPayType()).isEqualTo("offline_pay");
		assertThat(created.getTradeId()).isEqualTo("T-OFF-9");
	}

	@Test
	@DisplayName("幂等：同 company_id+trade_id 已有 PAY_AFTER_CANCEL 时不二次 insert")
	void createAbnormalTradeRefund_whenPendingExists_returnsExistingWithoutInsert() {
		AftersalesRefund existing = new AftersalesRefund();
		existing.setRefundBn(2_0260115999L);
		existing.setCompanyId(1L);
		existing.setTradeId("T-IDEM-1");
		existing.setRefundStatus("PAY_AFTER_CANCEL");
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(existing);

		AftersalesRefund result =
				service.createAbnormalTradeRefund(
						baseOrder(),
						baseTradeRow("T-IDEM-1", "wxpay", 100),
						AftersalesRefundService.REFUND_STATUS_DUP_PAY);

		assertThat(result).isSameAs(existing);
		verify(aftersalesRefundMapper, never()).insert(any(AftersalesRefund.class));
	}

	@Test
	@DisplayName("幂等：已 PROCESSING 返回既有单且不 insert")
	void createAbnormalTradeRefund_whenProcessingExists_returnsExistingWithoutInsert() {
		AftersalesRefund existing = new AftersalesRefund();
		existing.setRefundBn(2_0260115777L);
		existing.setCompanyId(1L);
		existing.setTradeId("T-PROCESSING");
		existing.setRefundStatus("PROCESSING");
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(existing);

		AftersalesRefund result =
				service.createAbnormalTradeRefund(
						baseOrder(),
						baseTradeRow("T-PROCESSING", "wxpay", 200),
						AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);

		assertThat(result).isSameAs(existing);
		assertThat(result.getRefundStatus()).isEqualTo("PROCESSING");
		verify(aftersalesRefundMapper, never()).insert(any(AftersalesRefund.class));
	}

	@Test
	@DisplayName("幂等：已 SUCCESS 返回既有单且不 insert")
	void createAbnormalTradeRefund_whenSuccessExists_returnsExisting() {
		AftersalesRefund existing = new AftersalesRefund();
		existing.setRefundBn(2_0260115888L);
		existing.setTradeId("T-SUCCESS");
		existing.setRefundStatus("SUCCESS");
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(existing);

		AftersalesRefund result =
				service.createAbnormalTradeRefund(
						baseOrder(),
						baseTradeRow("T-SUCCESS", "wxpay", 50),
						AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);

		assertThat(result.getRefundStatus()).isEqualTo("SUCCESS");
		verify(aftersalesRefundMapper, never()).insert(any(AftersalesRefund.class));
	}

	@Test
	@DisplayName("非法 refund_status 拒绝建单")
	void createAbnormalTradeRefund_rejectsNonAbnormalStatus() {
		assertThatThrownBy(
						() ->
								service.createAbnormalTradeRefund(
										baseOrder(), baseTradeRow("T-X", "wxpay", 1), "READY"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(
						() ->
								service.createAbnormalTradeRefund(
										baseOrder(), baseTradeRow("T-Y", "wxpay", 1), "AUDIT_SUCCESS"))
				.isInstanceOf(IllegalArgumentException.class);
		verify(aftersalesRefundMapper, never()).insert(any(AftersalesRefund.class));
	}

	private static Map<String, Object> baseOrder() {
		Map<String, Object> order = new LinkedHashMap<>();
		order.put("company_id", 1L);
		order.put("user_id", 42L);
		order.put("order_id", 9_001_234_567_890L);
		order.put("shop_id", 10L);
		order.put("distributor_id", 0L);
		order.put("pay_type", "wxpay");
		order.put("freight_type", "cash");
		return order;
	}

	private static Map<String, Object> wxTradeRow(String tradeId, int payFee) {
		return baseTradeRow(tradeId, "wxpay", payFee);
	}

	private static Map<String, Object> baseTradeRow(String tradeId, String payType, int payFee) {
		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", tradeId);
		trade.put("pay_type", payType);
		trade.put("pay_fee", payFee);
		trade.put("merchant_id", 0L);
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_rate", 1.0);
		trade.put("cur_fee_symbol", "￥");
		return trade;
	}
}