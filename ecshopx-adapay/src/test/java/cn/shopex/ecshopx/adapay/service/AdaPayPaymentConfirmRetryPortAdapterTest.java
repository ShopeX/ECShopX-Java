package cn.shopex.ecshopx.adapay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdaPayPaymentConfirmRetryPortAdapterTest {

	@Mock
	private AdapayAutoCloseAftersalesPaymentConfirmationService adapayAutoCloseAftersalesPaymentConfirmationService;

	@InjectMocks
	private AdaPayPaymentConfirmRetryPortAdapter adapter;

	@Test
	void delegatesToConfirmationService() {
		when(adapayAutoCloseAftersalesPaymentConfirmationService.adaPayPaymentConfirmRetry()).thenReturn(3);
		assertThat(adapter.adaPayPaymentConfirmRetry()).isEqualTo(3);
		verify(adapayAutoCloseAftersalesPaymentConfirmationService).adaPayPaymentConfirmRetry();
	}
}
