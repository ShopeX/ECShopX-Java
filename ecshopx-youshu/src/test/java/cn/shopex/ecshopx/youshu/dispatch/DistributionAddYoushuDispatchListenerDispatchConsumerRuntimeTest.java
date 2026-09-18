package cn.shopex.ecshopx.youshu.dispatch;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.youshu.service.YoushuDistributionAddSrDataSyncService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributionAddYoushuDispatchListenerDispatchConsumerRuntimeTest {

	@Mock
	private YoushuDistributionAddSrDataSyncService youshuDistributionAddSrDataSyncService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		DistributionAddYoushuDispatchListener listener =
				new DistributionAddYoushuDispatchListener(youshuDistributionAddSrDataSyncService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_ADD,
				DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION,
				ListenerDispatchOptions.async("default", null),
				listener);
		runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
	}

	@Test
	@DisplayName("EVENT_DISTRIBUTION_ADD: youshu listener — DispatchConsumerRuntime consume")
	void consume_event_message_invokes_sync_service_once() {
		long companyId = 7L;
		long distributorId = 99L;
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", companyId);
		entities.put("distributor_id", distributorId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						DistributionDispatchEventNames.EVENT_DISTRIBUTION_ADD,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-distribution-add-youshu",
						DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION);

		runtime.consume(message, 1);

		verify(youshuDistributionAddSrDataSyncService, times(1)).syncAfterDistributionAdd(eq(entities));
	}
}
