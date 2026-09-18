package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.hfpay.dispatch.DistributionEditHfEnterapplyInitDispatchListener;
import cn.shopex.ecshopx.hfpay.dispatch.EnterapplyInitLedger;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayEnterapplyInitAfterDistributionEditExecutionService;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DistributionEditHfpayEnterapplyEvent290AsyncBusDispatchFlowTest {

	@Test
	void publishEvent290_async_enqueuesHfEnterapplyEditListenerTask_andConsumerRuns() {
		HfpayEnterapplyReadService readSvc = mock(HfpayEnterapplyReadService.class);
		HfpayEnterapplyMapper mapper = mock(HfpayEnterapplyMapper.class);
		EnterapplyInitLedger ledger = new EnterapplyInitLedger(readSvc, mapper);
		HfpayEnterapplyInitAfterDistributionEditExecutionService execution =
				new HfpayEnterapplyInitAfterDistributionEditExecutionService(ledger);
		DistributionEditHfEnterapplyInitDispatchListener listener =
				new DistributionEditHfEnterapplyInitDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT_CSV290,
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT_EDIT,
				ListenerDispatchOptions.async("default", null),
				listener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 501L);
		entities.put("distributor_id", 902L);
		entities.put("is_open", "true");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		when(readSvc.getEnterapply(anyLong(), anyLong())).thenReturn(null);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT_CSV290,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		assertEquals(
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT_EDIT,
				captured.get(0).listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(mapper, times(1)).insert(any(HfpayEnterapply.class));
	}
}
