package cn.shopex.ecshopx.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.payment.BspayPaymentConfirmRetryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BsPayServiceTest {

	@Mock
	private BspayPaymentConfirmRetryPort bspayPaymentConfirmRetryPort;

	@InjectMocks
	private BsPayService bsPayService;

	@Test
	void scheduleRetryBsPayConfirm_delegates() {
		when(bspayPaymentConfirmRetryPort.scheduleRetryBsPayConfirm()).thenReturn(5);
		assertThat(bsPayService.scheduleRetryBsPayConfirm()).isEqualTo(5);
		verify(bspayPaymentConfirmRetryPort).scheduleRetryBsPayConfirm();
	}
}
