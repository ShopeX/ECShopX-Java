package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.TradeRefundPushMarketingCenterProcessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThirdPartyTradeAftersalesPushMarketingCenterAsyncBusDispatchFlowTest {

	@Test
	void publishEvent_tradeAftersales_async_enqueuesMarketingCenterTask_thenConsume_invokesProcessor() {
		TradeRefundPushMarketingCenterProcessor mockProcessor = mock(TradeRefundPushMarketingCenterProcessor.class);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		// Same order and ListenerDispatchOptions as SystemLinkTradeAftersalesDispatchListenerRegistrationConfig
		// (OME listener FQN omitted here to avoid ecshopx-third-party ↔ ecshopx-system-link Maven cycle)
		DispatchListener noopOme = payload -> {};
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_ome",
				ListenerDispatchOptions.asyncDefaults(),
				noopOme);
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				"listener:thirdparty.trade_aftersales_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				new ThirdPartyTradeAftersalesPushMarketingCenterDispatchListener(mockProcessor));

		List<DispatchMessage> captured = new ArrayList<>();
		SyncDispatchDriver syncDriver = new SyncDispatchDriver(registry);
		DispatchCore core =
				DispatchCore.asyncReady(registry, syncDriver, Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 55L);
		payload.put("aftersales_bn", 202601011234571L);

		facade.publishEvent(
				SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(2, captured.size());
		DispatchMessage marketing =
				captured.stream()
						.filter(
								m ->
										"listener:thirdparty.trade_aftersales_push_marketing_center"
												.equals(m.listenerName()))
						.findFirst()
						.orElseThrow();
		assertEquals(SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES, marketing.messageName());
		assertEquals("default", marketing.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(marketing, 1);

		verify(mockProcessor).handle(anyMap());
	}
}
