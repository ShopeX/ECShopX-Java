package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OfflinePaymentPayFeeSupport")
class OfflinePaymentPayFeeSupportTest {

	@Test
	void resolveUploadPayFee_usesOrderTotalWhenMissing() {
		assertEquals(9900L, OfflinePaymentPayFeeSupport.resolveUploadPayFee(null, 9900L));
	}

	@Test
	void resolveUploadPayFee_usesSubmittedWhenMatchesOrderTotal() {
		assertEquals(325000L, OfflinePaymentPayFeeSupport.resolveUploadPayFee("325000", 325000L));
	}

	@Test
	void resolveUploadPayFee_fallsBackWhenTampered() {
		assertEquals(9900L, OfflinePaymentPayFeeSupport.resolveUploadPayFee("100", 9900L));
	}
}
