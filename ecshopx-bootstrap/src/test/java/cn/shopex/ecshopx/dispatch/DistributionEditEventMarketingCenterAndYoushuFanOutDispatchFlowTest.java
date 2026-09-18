package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.thirdparty.dispatch.DistributionEditPushMarketingCenterDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.DistributionEditPushMarketingCenterProcessor;
import cn.shopex.ecshopx.youshu.dispatch.DistributionEditYoushuDispatchListener;
import cn.shopex.ecshopx.youshu.service.YoushuDistributionEditSrDataSyncService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DistributionEditEventMarketingCenterAndYoushuFanOutDispatchFlowTest {

	@Test
	void publishDistributionEdit_asyncRedis_enqueuesMarketingCenterThenYoushu_andConsumerInvokesBothInRegistrationOrder() {
		DistributionEditPushMarketingCenterProcessor mcProcessor = mock(DistributionEditPushMarketingCenterProcessor.class);
		DistributionEditPushMarketingCenterDispatchListener mcListener =
				new DistributionEditPushMarketingCenterDispatchListener(mcProcessor);

		YoushuDistributionEditSrDataSyncService syncService = mock(YoushuDistributionEditSrDataSyncService.class);
		DistributionEditYoushuDispatchListener youshuListener = new DistributionEditYoushuDispatchListener(syncService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT,
				DistributionDispatchEventNames.LISTENER_THIRDPARTY_DISTRIBUTION_EDIT_PUSH_MARKETING_CENTER,
				ListenerDispatchOptions.async("default", null),
				mcListener);
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT,
				DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION,
				ListenerDispatchOptions.async("default", null),
				youshuListener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 7L;
		long distributorId = 55L;
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

		assertEquals(2, captured.size());
		assertEquals(
				DistributionDispatchEventNames.LISTENER_THIRDPARTY_DISTRIBUTION_EDIT_PUSH_MARKETING_CENTER,
				captured.get(0).listenerName());
		assertEquals(DistributionDispatchEventNames.LISTENER_YOUSHU_BUNDLE_DISTRIBUTION, captured.get(1).listenerName());
		assertEquals("default", captured.get(0).queue());
		assertEquals("default", captured.get(1).queue());
		assertEquals(entities, captured.get(0).payload().get("entities"));
		assertEquals(entities, captured.get(1).payload().get("entities"));

		DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(
				registry,
				new DispatchRetryDecider(),
				mock(FailedJobRecorder.class),
				mock(DispatchStructuredLogger.class),
				new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);

		InOrder order = inOrder(mcProcessor, syncService);
		order.verify(mcProcessor)
				.handle(argThat(DistributionEditEventMarketingCenterAndYoushuFanOutDispatchFlowTest::matchesDistributionEditPayload));
		order.verify(syncService).syncAfterDistributionEdit(eq(entities));
	}

	private static boolean matchesDistributionEditPayload(Map<String, Object> m) {
		if (m == null) {
			return false;
		}
		Object raw = m.get("entities");
		if (!(raw instanceof Map<?, ?> entities)) {
			return false;
		}
		Object c = entities.get("company_id");
		Object d = entities.get("distributor_id");
		if (!(c instanceof Number) || !(d instanceof Number)) {
			return false;
		}
		return ((Number) c).longValue() == 7L && ((Number) d).longValue() == 55L;
	}
}
