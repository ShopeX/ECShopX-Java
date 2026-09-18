package cn.shopex.ecshopx.bspay.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.bspay.service.BspayAutoCloseAftersalesPaymentConfirmationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BspayPaymentConfirmRetryPortAdapterTest {

	@Mock
	private BspayAutoCloseAftersalesPaymentConfirmationService bspayAutoCloseAftersalesPaymentConfirmationService;

	@InjectMocks
	private BspayPaymentConfirmRetryPortAdapter adapter;

	@Test
	void delegatesToConfirmationService() {
		when(bspayAutoCloseAftersalesPaymentConfirmationService.scheduleRetryBsPayConfirm()).thenReturn(3);
		assertThat(adapter.scheduleRetryBsPayConfirm()).isEqualTo(3);
		verify(bspayAutoCloseAftersalesPaymentConfirmationService).scheduleRetryBsPayConfirm();
	}
}
