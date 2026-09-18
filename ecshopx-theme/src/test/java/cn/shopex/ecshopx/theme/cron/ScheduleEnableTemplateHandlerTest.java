package cn.shopex.ecshopx.theme.cron;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.theme.service.PagesTemplateServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleEnableTemplateHandlerTest {

	@Mock
	private PagesTemplateServices pagesTemplateServices;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleEnableTemplateHandler handler;

	@Test
	void execute_ok() {
		handler.execute();
		verify(pagesTemplateServices).scheduleEnableTemplate();
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void execute_failure_publishesCronAlertAndRethrows() {
		RuntimeException ex = new RuntimeException("boom");
		doThrow(ex).when(pagesTemplateServices).scheduleEnableTemplate();
		assertThrows(RuntimeException.class, () -> handler.execute());
		verify(eventPublisher)
				.publishEvent(
						argThat(
								ev ->
										ev instanceof CronAlertEvent ce
												&& "enable-theme-template"
														.equals(ce.getHandlerShortName())
												&& ce.getCause() == ex));
	}
}
