package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AftersalesRefundServiceTradeFinishAbnormalRefundIsolationTest {

	@Test
	@DisplayName("§4.4 异常待审状态识别")
	void isTradeFinishAbnormalRefund_pendingStatuses() {
		AftersalesRefund payAfterCancel = new AftersalesRefund();
		payAfterCancel.setRefundStatus(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);
		assertThat(AftersalesRefundService.isTradeFinishAbnormalRefund(payAfterCancel))
				.isTrue();

		AftersalesRefund dupPay = new AftersalesRefund();
		dupPay.setRefundStatus(AftersalesRefundService.REFUND_STATUS_DUP_PAY);
		assertThat(AftersalesRefundService.isTradeFinishAbnormalRefund(dupPay)).isTrue();
	}

	@Test
	@DisplayName("§4.4 普通 READY 售前取消不视为异常单")
	void isTradeFinishAbnormalRefund_presaleReady() {
		AftersalesRefund ready = new AftersalesRefund();
		ready.setRefundStatus("READY");
		ready.setRefundType("1");
		ready.setReturnFreight(1);
		assertThat(AftersalesRefundService.isTradeFinishAbnormalRefund(ready)).isFalse();
	}

	@Test
	@DisplayName("§4.4 异常 PROCESSING（无售后单、return_freight=0）仍隔离")
	void isTradeFinishAbnormalRefund_abnormalProcessing() {
		AftersalesRefund abnormal = new AftersalesRefund();
		abnormal.setRefundStatus("PROCESSING");
		abnormal.setRefundType("1");
		abnormal.setReturnFreight(0);
		assertThat(AftersalesRefundService.isTradeFinishAbnormalRefund(abnormal)).isTrue();

		AftersalesRefund presale = new AftersalesRefund();
		presale.setRefundStatus("PROCESSING");
		presale.setRefundType("1");
		presale.setReturnFreight(1);
		assertThat(AftersalesRefundService.isTradeFinishAbnormalRefund(presale)).isFalse();
	}

	@Test
	void refundStatusLabel_includesAbnormalStatuses() {
		assertThat(AftersalesRefundService.refundStatusLabel(
						AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL))
				.isEqualTo("逾期支付待退款");
		assertThat(AftersalesRefundService.refundStatusLabel(
						AftersalesRefundService.REFUND_STATUS_DUP_PAY))
				.isEqualTo("重复支付待退款");
	}
}