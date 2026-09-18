package cn.shopex.ecshopx.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.payment.AdaPayPaymentConfirmRetryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdaPaymentServiceTest {

	@Mock
	private AdaPayPaymentConfirmRetryPort adaPayPaymentConfirmRetryPort;

	@InjectMocks
	private AdaPaymentService adaPaymentService;

	@Test
	void adaPayPaymentConfirmRetry_delegates() {
		when(adaPayPaymentConfirmRetryPort.adaPayPaymentConfirmRetry()).thenReturn(5);
		assertThat(adaPaymentService.adaPayPaymentConfirmRetry()).isEqualTo(5);
		verify(adaPayPaymentConfirmRetryPort).adaPayPaymentConfirmRetry();
	}
}
