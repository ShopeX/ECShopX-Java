package cn.shopex.ecshopx.orders.service.front.userinvoice;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SendInvoiceEmailJobDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class UserInvoiceResendEmailServiceDispatchTest {

	@Mock
	private OrderInvoiceMapper orderInvoiceMapper;

	@Mock
	private UserInvoiceResendEmailTxService userInvoiceResendEmailTxService;

	@Mock
	private SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher;

	@Mock
	private MessageSource messageSource;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private UserInvoiceResendEmailService service;

	@BeforeEach
	void setUp() {
		service = new UserInvoiceResendEmailService(
				orderInvoiceMapper,
				userInvoiceResendEmailTxService,
				sendInvoiceEmailJobDispatchPublisher,
				messageSource,
				objectMapper);
		when(messageSource.getMessage(anyString(), isNull(), anyString(), any(Locale.class)))
				.thenAnswer(invocation -> invocation.getArgument(2));
	}

	@Test
	void resendInvoiceEmail_afterValidation_publishesSendInvoiceEmailJob() {
		OrderInvoice row = new OrderInvoice();
		row.setId(10L);
		row.setUserId(1L);
		row.setCompanyId(9L);
		row.setEmail("a@b.com");
		row.setInvoiceFileUrl("https://example.com/f.pdf");

		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("confirm_email", "a@b.com");
		merged.put("id", "10");

		Map<String, Object> out = service.resendInvoiceEmail(1L, 9L, merged);

		assertNotNull(out.get("message"));
		verify(sendInvoiceEmailJobDispatchPublisher)
				.publish(eq("a@b.com"), eq("https://example.com/f.pdf"), eq(9L));
		verify(userInvoiceResendEmailTxService, never())
				.applyEmailChangeAndLog(any(Long.class), any(), anyString(), any(Long.class), anyString());
	}
}
