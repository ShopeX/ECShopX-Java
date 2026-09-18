package cn.shopex.ecshopx.aftersales.support;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AftersalesRefundEntityTradeRefundPayloadMapperTest {

	private final AftersalesRefundEntityTradeRefundPayloadMapper mapper =
			new AftersalesRefundEntityTradeRefundPayloadMapper();

	@Test
	void toDispatchPayload_mapsAllCoreKeys() {
		AftersalesRefund r = new AftersalesRefund();
		r.setRefundBn(91001L);
		r.setAftersalesBn(202601071234567L);
		r.setOrderId(88001L);
		r.setTradeId("trade-x");
		r.setCompanyId(42L);
		r.setSupplierId(3L);
		r.setUserId(99L);
		r.setShopId(11L);
		r.setDistributorId(22L);
		r.setRefundType("0");
		r.setRefundChannel("original");
		r.setRefundStatus("READY");
		r.setRefundFee(500);
		r.setRefundPoint(10);
		r.setReturnFreight(1);
		r.setFreight(20);
		r.setFreightType("cash");
		r.setPayType("online");
		r.setCurrency("CNY");
		r.setCurFeeType("CNY");
		r.setCurFeeRate(1.25);
		r.setCurFeeSymbol("¥");
		r.setCurPayFee("500");
		r.setMerchantId(7L);
		r.setReturnPoint(5);

		Map<String, Object> m = mapper.toDispatchPayload(r);

		assertThat(m.get("refund_bn")).isEqualTo(91001L);
		assertThat(m.get("aftersales_bn")).isEqualTo(202601071234567L);
		assertThat(m.get("order_id")).isEqualTo(88001L);
		assertThat(m.get("trade_id")).isEqualTo("trade-x");
		assertThat(m.get("company_id")).isEqualTo(42L);
		assertThat(m.get("supplier_id")).isEqualTo(3L);
		assertThat(m.get("user_id")).isEqualTo(99L);
		assertThat(m.get("shop_id")).isEqualTo(11L);
		assertThat(m.get("distributor_id")).isEqualTo(22L);
		assertThat(m.get("refund_type")).isEqualTo(0);
		assertThat(m.get("refund_channel")).isEqualTo("original");
		assertThat(m.get("refund_status")).isEqualTo("READY");
		assertThat(m.get("refund_fee")).isEqualTo(500);
		assertThat(m.get("refund_point")).isEqualTo(10);
		assertThat(m.get("return_freight")).isEqualTo(1);
		assertThat(m.get("freight")).isEqualTo(20);
		assertThat(m.get("freight_type")).isEqualTo("cash");
		assertThat(m.get("pay_type")).isEqualTo("online");
		assertThat(m.get("currency")).isEqualTo("CNY");
		assertThat(m.get("cur_fee_type")).isEqualTo("CNY");
		assertThat(m.get("cur_fee_rate")).isEqualTo(1.25);
		assertThat(m.get("cur_fee_symbol")).isEqualTo("¥");
		assertThat(m.get("cur_pay_fee")).isEqualTo("500");
		assertThat(m.get("merchant_id")).isEqualTo(7L);
		assertThat(m.get("return_point")).isEqualTo(5);
	}

	@Test
	void toDispatchPayload_omitsReturnPointWhenZeroOrNull() {
		AftersalesRefund r = new AftersalesRefund();
		r.setRefundBn(1L);
		r.setAftersalesBn(2L);
		r.setOrderId(3L);
		r.setTradeId("t");
		r.setCompanyId(4L);
		r.setRefundChannel("offline");
		r.setRefundStatus("READY");
		r.setReturnPoint(0);

		Map<String, Object> m = mapper.toDispatchPayload(r);
		assertThat(m).doesNotContainKey("return_point");

		r.setReturnPoint(null);
		Map<String, Object> m2 = mapper.toDispatchPayload(r);
		assertThat(m2).doesNotContainKey("return_point");
	}
}
