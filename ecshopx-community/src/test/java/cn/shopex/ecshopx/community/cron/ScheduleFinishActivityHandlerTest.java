package cn.shopex.ecshopx.community.cron;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.community.service.CommunityActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ScheduleFinishActivityHandlerTest {

	@Mock
	private CommunityActivityService communityActivityService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private ScheduleFinishActivityHandler handler;

	@Test
	void okPath_logsProcessed() {
		when(communityActivityService.scheduleFinishActivity()).thenReturn(3);
		handler.execute();
		verify(communityActivityService).scheduleFinishActivity();
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void errPath_publishesAndRethrows() {
		RuntimeException ex = new RuntimeException("x");
		when(communityActivityService.scheduleFinishActivity()).thenThrow(ex);
		assertThatThrownBy(() -> handler.execute()).isSameAs(ex);
		ArgumentCaptor<CronAlertEvent> cap = ArgumentCaptor.forClass(CronAlertEvent.class);
		verify(eventPublisher).publishEvent(cap.capture());
		CronAlertEvent event = cap.getValue();
		org.assertj.core.api.Assertions.assertThat(event.getHandlerShortName()).isEqualTo("finish-community-activity");
		verifyNoMoreInteractions(eventPublisher);
	}
}
