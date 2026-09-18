package cn.shopex.ecshopx.systemlink.cron;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.systemlink.service.wdterp.WdtErpSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleSyncLogisticsHandlerTest {

	@Mock
	WdtErpSyncService wdtErpSyncService;

	@Mock
	ApplicationEventPublisher eventPublisher;

	@InjectMocks
	ScheduleSyncLogisticsHandler handler;

	@Test
	void execute_success_noAlert() {
		handler.execute();
		verify(wdtErpSyncService).scheduleSyncLogistics();
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void execute_failure_publishesCronAlert() {
		RuntimeException boom = new RuntimeException("t");
		doThrow(boom).when(wdtErpSyncService).scheduleSyncLogistics();
		assertThrows(RuntimeException.class, () -> handler.execute());
		verify(eventPublisher).publishEvent(any(CronAlertEvent.class));
	}
}
