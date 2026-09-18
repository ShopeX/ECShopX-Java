package cn.shopex.ecshopx.hfpay.dispatch;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributionEditHfEnterapplyInitDispatchListenerDispatchConsumerRuntimeTest {

	private HfpayEnterapplyReadService readSvc;
	private HfpayEnterapplyMapper mapper;
	private EnterapplyInitLedger ledger;
	private HfpayEnterapplyInitAfterDistributionEditExecutionService executionService;
	private DispatchConsumerRuntime runtime;

	@BeforeEach
	void setUp() {
		readSvc = mock(HfpayEnterapplyReadService.class);
		mapper = mock(HfpayEnterapplyMapper.class);
		ledger = new EnterapplyInitLedger(readSvc, mapper);
		executionService = new HfpayEnterapplyInitAfterDistributionEditExecutionService(ledger);
		DistributionEditHfEnterapplyInitDispatchListener listener =
				new DistributionEditHfEnterapplyInitDispatchListener(executionService);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT_CSV290,
				DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT_EDIT,
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
	@DisplayName("event:290 DistributionEditEvent — HfEnterapplyInit@edit listener via DispatchConsumerRuntime")
	void consume_dispatchesToEditExecutionService() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("distributor_id", 33L);
		entities.put("company_id", 501L);
		entities.put("is_open", "true");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		when(readSvc.getEnterapply(anyLong(), anyLong())).thenReturn(null);

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT_CSV290,
						payload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-distribution-edit-hfpay-290",
						DistributionDispatchEventNames.LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT_EDIT);

		runtime.consume(message, 1);

		verify(readSvc, times(1)).getEnterapply(eq(501L), eq(33L));
		verify(mapper).insert(org.mockito.ArgumentMatchers.any(HfpayEnterapply.class));
	}
}
