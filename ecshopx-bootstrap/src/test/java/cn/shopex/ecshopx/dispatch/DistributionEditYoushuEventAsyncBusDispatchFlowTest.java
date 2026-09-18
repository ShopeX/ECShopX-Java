package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.youshu.dispatch.DistributionEditYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuDistributionEditSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Baseline async fan-out for {@code EVENT_DISTRIBUTION_EDIT} and the Youshu listener; same {@code messageName}
 * and {@code listenerName} as shop-batch distributor updates ({@code entry-02-api-distributorshop-updateshops}).
 */
class DistributionEditYoushuEventAsyncBusDispatchFlowTest {

	@Test
	@DisplayName("EVENT_DISTRIBUTION_EDIT async flow — shop-batch distributor updates (entry-02-api-distributorshop-updateshops)")
	void publishDistributionEdit_async_enqueuesOneListenerTask_andConsumerRuns() {
		YoushuDistributionEditSrDataSyncService youshuSvc = mock(YoushuDistributionEditSrDataSyncService.class);
		DistributionEditYoushuDispatchListener youshuListener = new DistributionEditYoushuDispatchListener(youshuSvc);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT,
				DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION,
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 100L;
		long distributorId = 200L;
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", companyId);
		entities.put("distributor_id", distributorId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		assertEquals(DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION, captured.get(0).listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(youshuSvc).syncAfterDistributionEdit(eq(entities));
	}
}
