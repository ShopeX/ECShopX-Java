package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import cn.shopex.ecshopx.hfpay.dispatch.DistributionAddHfEnterapplyInitDispatchListener;
import cn.shopex.ecshopx.hfpay.dispatch.EnterapplyInitLedger;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayEnterapplyInitAfterDistributionAddExecutionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DistributionAddHfpayEnterapplyEvent289AsyncBusDispatchFlowTest {

	@Test
	void publishEvent289_async_enqueuesHfEnterapplyListenerTask_andConsumerRuns() {
		EnterapplyInitLedger ledger =
				new EnterapplyInitLedger(mock(HfpayEnterapplyReadService.class), mock(HfpayEnterapplyMapper.class));
		HfpayEnterapplyInitAfterDistributionAddExecutionService execution =
				new HfpayEnterapplyInitAfterDistributionAddExecutionService(ledger);
		DistributionAddHfEnterapplyInitDispatchListener listener =
				new DistributionAddHfEnterapplyInitDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_ADD_CSV289,
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT,
				ListenerDispatchOptions.async("default", null),
				listener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 501L);
		entities.put("card_id", "card-flow-289");
		entities.put("distributor_id", 902L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_ADD_CSV289,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		assertEquals(
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT,
				captured.get(0).listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		EnterapplyInitLedger.CardCompanyRef recorded =
				ledger.lastRecorded().orElseThrow(() -> new AssertionError("expected ledger record after consume"));
		assertEquals("card-flow-289", recorded.cardId());
		assertEquals("501", recorded.companyId());
	}
}
