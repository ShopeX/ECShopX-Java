package cn.shopex.ecshopx.hfpay.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DistributionAddHfEnterapplyInitDispatchListenerDispatchConsumerRuntimeTest {

	private EnterapplyInitLedger ledger;
	private HfpayEnterapplyInitAfterDistributionAddExecutionService executionService;

	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		ledger = new EnterapplyInitLedger(mock(HfpayEnterapplyReadService.class), mock(HfpayEnterapplyMapper.class));
		executionService = new HfpayEnterapplyInitAfterDistributionAddExecutionService(ledger);
		DistributionAddHfEnterapplyInitDispatchListener listener =
				new DistributionAddHfEnterapplyInitDispatchListener(executionService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_ADD_CSV289,
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT,
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
	@DisplayName("event:289 DistributionAddEvent — HfEnterapplyInit listener via DispatchConsumerRuntime")
	void consume_event289_child_invokes_execution_service_once() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("distributor_id", 33L);
		entities.put("cardId", "card-rt-289");
		entities.put("company_id", "co-77");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						DistributionDispatchEventNames.EVENT_DISTRIBUTION_ADD_CSV289,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-distribution-add-hfpay-289",
						DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT);

		runtime.consume(message, 1);

		EnterapplyInitLedger.CardCompanyRef recorded =
				ledger.lastRecorded().orElseThrow(() -> new AssertionError("expected ledger record"));
		assertEquals("card-rt-289", recorded.cardId());
		assertEquals("co-77", recorded.companyId());
	}
}
