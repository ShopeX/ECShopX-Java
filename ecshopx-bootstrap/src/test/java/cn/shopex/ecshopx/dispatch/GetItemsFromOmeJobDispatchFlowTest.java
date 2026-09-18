package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.config.GetItemsFromOmeJobDispatchPublisherImpl;
import cn.shopex.ecshopx.goods.dispatch.GetItemsFromOmeJobDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.GetItemsFromOmeJobHandler;
import cn.shopex.ecshopx.goods.service.ome.OmeItemsFromOmePersistService;
import cn.shopex.ecshopx.goods.service.ome.OmeItemsFromOmePagedSyncRunner;
import cn.shopex.ecshopx.goods.service.ome.OmeLastTimeRedisAccessor;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpOpenApiClient;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpSettingRedisAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Dispatch-bus tests for GetItemsFromOme: async Redis driver, slow queue, payload envelope, and consumer-driven
 * paging. Covers the job-chain where each consume may enqueue the next page via
 * {@link GetItemsFromOmeJobDispatchPublisher} (same job name and {@code DispatchOptions} as the initial enqueue).
 * Initial HTTP enqueue behaviour is covered by {@code OmeItemsSyncFacadeInitialDispatchTest} in module
 * {@code ecshopx-goods}.
 * Migration artifact slug reference for traceability: entry-02.
 */
@DisplayName("GetItemsFromOme job-chain (bus paging and self-dispatch of next page)")
class GetItemsFromOmeJobDispatchFlowTest {

	private static final DateTimeFormatter OME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerRunsOmeListCall() {
		long companyId = 10L;
		int page = 1;
		long endUnix = 1_700_003_600L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getItemsCursorUnix(companyId)).thenReturn(startUnix);

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rsp", "succ");
		data.put("count", 0);
		data.put("list", List.of());
		when(openApi.call(eq(companyId), eq("goods.getList"), any())).thenReturn(Map.of("data", data));

		OmeItemsFromOmePersistService persist = mock(OmeItemsFromOmePersistService.class);
		ObjectMapper om = new ObjectMapper();
		GetItemsFromOmeJobDispatchPublisher pub = mock(GetItemsFromOmeJobDispatchPublisher.class);

		OmeItemsFromOmePagedSyncRunner runner =
				new OmeItemsFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, pub);
		GetItemsFromOmeJobHandler handler = new GetItemsFromOmeJobHandler(runner);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);
		payload.put("goods_bn", "");

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(page, asInt(got.get("page")));
		assertEquals(endUnix, asLong(got.get("end_lastmodify_unix")));
		assertEquals("", got.get("goods_bn"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> paramsCap = ArgumentCaptor.forClass(Map.class);
		verify(openApi, times(1)).call(eq(companyId), eq("goods.getList"), paramsCap.capture());
		Map<String, Object> p = paramsCap.getValue();
		assertEquals(page, p.get("page_no"));
		assertEquals(10, p.get("page_size"));
		ZoneId z = ZoneId.systemDefault();
		String expectStart = Instant.ofEpochSecond(startUnix).atZone(z).format(OME_TIME);
		String expectEnd = Instant.ofEpochSecond(endUnix).atZone(z).format(OME_TIME);
		assertEquals(expectStart, p.get("start_lastmodify"));
		assertEquals(expectEnd, p.get("end_lastmodify"));
	}

	/**
	 * Runner-level evidence: mocked publisher receives {@code page + 1} when OME reports more rows than fit on the
	 * current page (same metadata as real bus path; no nested {@code DispatchFacade} here).
	 */
	@Test
	void consumeQueuedPage_whenMorePages_remain_enqueuesPagePlusOneOnPublisher() {
		long companyId = 11L;
		long endUnix = 1_700_010_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getItemsCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("bn", "sku-1");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rsp", "succ");
		data.put("count", 25);
		data.put("list", List.of(item));

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goods.getList"), any())).thenReturn(Map.of("data", data));

		OmeItemsFromOmePersistService persist = mock(OmeItemsFromOmePersistService.class);
		ObjectMapper om = new ObjectMapper();
		GetItemsFromOmeJobDispatchPublisher pub = mock(GetItemsFromOmeJobDispatchPublisher.class);

		OmeItemsFromOmePagedSyncRunner runner =
				new OmeItemsFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, pub);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", 1);
		payload.put("end_lastmodify_unix", endUnix);
		payload.put("goods_bn", "");

		runner.consumeQueuedPage(payload);

		verify(pub, times(1)).enqueueGetItemsFromOme(eq(companyId), eq(2), eq(endUnix), eq(""));
		verify(lastTime, never()).setItemsCursorUnix(anyLong(), anyLong());
		verify(persist, times(1))
				.persistOmeGoodsPage(eq(companyId), any(), eq("goods.getList"), eq(1), eq(endUnix), eq(""));
	}

	@Test
	void consumeQueuedPage_whenCurrentPageIsLast_updatesCursorAndDoesNotEnqueueNext() {
		long companyId = 12L;
		long endUnix = 1_700_020_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getItemsCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("bn", "sku-1");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rsp", "succ");
		data.put("count", 25);
		data.put("list", List.of(item));

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goods.getList"), any())).thenReturn(Map.of("data", data));

		OmeItemsFromOmePersistService persist = mock(OmeItemsFromOmePersistService.class);
		ObjectMapper om = new ObjectMapper();
		GetItemsFromOmeJobDispatchPublisher pub = mock(GetItemsFromOmeJobDispatchPublisher.class);

		OmeItemsFromOmePagedSyncRunner runner =
				new OmeItemsFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, pub);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", 3);
		payload.put("end_lastmodify_unix", endUnix);
		payload.put("goods_bn", "");

		runner.consumeQueuedPage(payload);

		verify(lastTime, times(1)).setItemsCursorUnix(eq(companyId), eq(endUnix));
		verify(pub, never()).enqueueGetItemsFromOme(anyLong(), anyInt(), anyLong(), any());
		verify(persist, times(1))
				.persistOmeGoodsPage(eq(companyId), any(), eq("goods.getList"), eq(3), eq(endUnix), eq(""));
	}

	/**
	 * Full-chain evidence: first {@code dispatchJob} is captured, {@link DispatchConsumerRuntime#consume} runs the
	 * handler, then {@link GetItemsFromOmeJobDispatchPublisherImpl} appends a second async job with {@code page=2}
	 * on the slow Redis queue.
	 */
	@Test
	void dispatchJob_afterFirstPageConsumes_enqueuesSecondJobWithPageTwoOnSlowQueue() {
		long companyId = 13L;
		int page = 1;
		long endUnix = 1_700_030_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getItemsCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("bn", "sku-1");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rsp", "succ");
		data.put("count", 25);
		data.put("list", List.of(item));

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goods.getList"), any())).thenReturn(Map.of("data", data));

		OmeItemsFromOmePersistService persist = mock(OmeItemsFromOmePersistService.class);
		ObjectMapper om = new ObjectMapper();

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		GetItemsFromOmeJobDispatchPublisherImpl publisher = new GetItemsFromOmeJobDispatchPublisherImpl(facade);
		OmeItemsFromOmePagedSyncRunner runner =
				new OmeItemsFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, publisher);
		GetItemsFromOmeJobHandler handler = new GetItemsFromOmeJobHandler(runner);
		registry.registerJob(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);
		payload.put("goods_bn", "");

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage first = captured.get(0);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(first, 1);

		assertEquals(2, captured.size());
		DispatchMessage second = captured.get(1);
		assertEquals(DispatchMessageType.JOB, second.messageType());
		assertEquals(DispatchMode.ASYNC, second.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, second.driverType());
		assertEquals("slow", second.queue());
		assertEquals(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME, second.messageName());

		Map<String, Object> p2 = second.payload();
		assertEquals(companyId, asLong(p2.get("company_id")));
		assertEquals(2, asInt(p2.get("page")));
		assertEquals(endUnix, asLong(p2.get("end_lastmodify_unix")));
		assertEquals("", p2.get("goods_bn"));
	}

	/**
	 * Third hop on the same chain: initial payload {@code page=2} with {@code count=25} and page size 10 still has
	 * remaining rows, so after consume the captured list gains a follow-up job with {@code page=3}.
	 */
	@Test
	void dispatchJob_afterPageTwoConsumes_enqueuesPageThreeOnSlowQueue() {
		long companyId = 14L;
		int page = 2;
		long endUnix = 1_700_040_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getItemsCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("bn", "sku-1");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rsp", "succ");
		data.put("count", 25);
		data.put("list", List.of(item));

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goods.getList"), any())).thenReturn(Map.of("data", data));

		OmeItemsFromOmePersistService persist = mock(OmeItemsFromOmePersistService.class);
		ObjectMapper om = new ObjectMapper();

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		GetItemsFromOmeJobDispatchPublisherImpl publisher = new GetItemsFromOmeJobDispatchPublisherImpl(facade);
		OmeItemsFromOmePagedSyncRunner runner =
				new OmeItemsFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, publisher);
		GetItemsFromOmeJobHandler handler = new GetItemsFromOmeJobHandler(runner);
		registry.registerJob(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);
		payload.put("goods_bn", "");

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage first = captured.get(0);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(first, 1);

		assertEquals(2, captured.size());
		DispatchMessage followUp = captured.get(1);
		assertEquals(DispatchMessageType.JOB, followUp.messageType());
		assertEquals(DispatchMode.ASYNC, followUp.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, followUp.driverType());
		assertEquals("slow", followUp.queue());
		assertEquals(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME, followUp.messageName());

		Map<String, Object> p3 = followUp.payload();
		assertEquals(companyId, asLong(p3.get("company_id")));
		assertEquals(3, asInt(p3.get("page")));
		assertEquals(endUnix, asLong(p3.get("end_lastmodify_unix")));
		assertEquals("", p3.get("goods_bn"));
	}

	@Test
	void dispatchJob_publishPayloadMatchesGetItemsFromOmeEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GetItemsFromOmeJobDispatchPublisherImpl publisher =
				new GetItemsFromOmeJobDispatchPublisherImpl(dispatchFacade);

		long companyId = 55L;
		long endUnix = 1_735_689_600L;
		publisher.enqueueGetItemsFromOme(companyId, 1, endUnix, "");

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME),
						argThat(
								map -> {
									if (companyId != asLong(map.get("company_id"))) {
										return false;
									}
									if (1 != asInt(map.get("page"))) {
										return false;
									}
									if (endUnix != asLong(map.get("end_lastmodify_unix"))) {
										return false;
									}
									if (!"".equals(map.get("goods_bn"))) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int asInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
