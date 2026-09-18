package cn.shopex.ecshopx.orders.service.invoice;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SendInvoiceEmailJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
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
class InvoiceResendEmailServiceDispatchTest {

	@Mock
	private OrderInvoiceMapper orderInvoiceMapper;

	@Mock
	private MessageSource messageSource;

	@Mock
	private SendInvoiceEmailJobDispatchPublisher sendInvoiceEmailJobDispatchPublisher;

	private InvoiceResendEmailService service;

	@BeforeEach
	void setUp() {
		service = new InvoiceResendEmailService(orderInvoiceMapper, messageSource, sendInvoiceEmailJobDispatchPublisher);
		lenient()
				.when(messageSource.getMessage(anyString(), isNull(), anyString(), any(Locale.class)))
				.thenAnswer(invocation -> invocation.getArgument(2));
	}

	@Test
	void resendInvoice_afterValidation_publishesWithExpectedChineseSubject() {
		OrderInvoice row = new OrderInvoice();
		row.setId(10L);
		row.setCompanyId(9L);
		row.setInvoiceStatus("success");
		row.setInvoiceFileUrl("  https://example.com/f.pdf  ");

		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("id", "10");
		merged.put("confirm_email", "  user@example.com  ");

		service.resendInvoice(9L, merged);

		verify(sendInvoiceEmailJobDispatchPublisher)
				.publish(
						eq("user@example.com"),
						eq("https://example.com/f.pdf"),
						eq(9L),
						eq("您的电子发票已生成"));
	}

	@Test
	void resendInvoice_whenInvoiceInvalid_doesNotPublish() {
		OrderInvoice row = new OrderInvoice();
		row.setId(10L);
		row.setCompanyId(9L);
		row.setInvoiceStatus("pending");
		row.setInvoiceFileUrl("https://example.com/f.pdf");

		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("id", "10");
		merged.put("confirm_email", "user@example.com");

		assertThrows(ResourceException.class, () -> service.resendInvoice(9L, merged));

		verifyNoInteractions(sendInvoiceEmailJobDispatchPublisher);
	}

	@Test
	void resendInvoice_whenUrlMissing_doesNotPublish() {
		OrderInvoice row = new OrderInvoice();
		row.setId(10L);
		row.setCompanyId(9L);
		row.setInvoiceStatus("success");
		row.setInvoiceFileUrl("");

		when(orderInvoiceMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("id", "10");
		merged.put("confirm_email", "user@example.com");

		assertThrows(ResourceException.class, () -> service.resendInvoice(9L, merged));

		verify(sendInvoiceEmailJobDispatchPublisher, never()).publish(anyString(), anyString(), anyLong());
		verify(sendInvoiceEmailJobDispatchPublisher, never())
				.publish(anyString(), anyString(), anyLong(), anyString());
	}
}
