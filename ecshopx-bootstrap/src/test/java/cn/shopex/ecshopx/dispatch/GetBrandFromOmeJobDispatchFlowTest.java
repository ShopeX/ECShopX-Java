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
import cn.shopex.ecshopx.config.GetBrandFromOmeJobDispatchPublisherImpl;
import cn.shopex.ecshopx.goods.dispatch.GetBrandFromOmeJobDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.GetBrandFromOmeJobHandler;
import cn.shopex.ecshopx.goods.service.ome.OmeBrandBatchPersistService;
import cn.shopex.ecshopx.goods.service.ome.OmeBrandFromOmePagedSyncRunner;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * End-to-end dispatch and consumer flow for {@code GetBrandFromOme} (publisher,
 * handler, chained pages). HTTP admin first-page enqueue is covered separately by
 * {@code cn.shopex.ecshopx.goods.service.ome.OmeBrandSyncFacadeInitialDispatchTest}
 * in {@code ecshopx-goods}; keep both suites when changing brand sync wiring.
 * <p>
 * Paged self-chain after dequeue uses {@code GetBrandFromOmeJobDispatchPublisher} and the same
 * {@code DispatchOptions} / {@code DispatchMessage} shape as the initial {@code dispatchJob}. Inventory anchor
 * {@code entry-02-jc-getbrandfromome-handle} (legacy {@code GetBrandFromOme::handle} pagination branch, lines ~82–87
 * in the source bundle).
 */
class GetBrandFromOmeJobDispatchFlowTest {

	private static final DateTimeFormatter OME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerRunsGoodsbrandGetList() {
		long companyId = 10L;
		int page = 1;
		long endUnix = 1_700_003_600L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getBrandCursorUnix(companyId)).thenReturn(startUnix);

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("count", 0);
		Map<String, Object> apiWrap = new LinkedHashMap<>();
		apiWrap.put("rsp", "succ");
		apiWrap.put("data", data);
		when(openApi.call(eq(companyId), eq("goodsbrand.getList"), any())).thenReturn(apiWrap);

		OmeBrandBatchPersistService persist = mock(OmeBrandBatchPersistService.class);
		ObjectMapper om = new ObjectMapper();
		GetBrandFromOmeJobDispatchPublisher pub = mock(GetBrandFromOmeJobDispatchPublisher.class);

		OmeBrandFromOmePagedSyncRunner runner =
				new OmeBrandFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, pub);
		GetBrandFromOmeJobHandler handler = new GetBrandFromOmeJobHandler(runner);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME,
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
		assertEquals(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(page, asInt(got.get("page")));
		assertEquals(endUnix, asLong(got.get("end_lastmodify_unix")));

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
		verify(openApi, times(1)).call(eq(companyId), eq("goodsbrand.getList"), paramsCap.capture());
		Map<String, Object> p = paramsCap.getValue();
		assertEquals(page, p.get("page_no"));
		assertEquals(10, p.get("page_size"));
		ZoneId z = ZoneId.systemDefault();
		String expectStart = Instant.ofEpochSecond(startUnix).atZone(z).format(OME_TIME);
		String expectEnd = Instant.ofEpochSecond(endUnix).atZone(z).format(OME_TIME);
		assertEquals(expectStart, p.get("start_time"));
		assertEquals(expectEnd, p.get("end_time"));
	}

	@Test
	void dispatchJob_publishPayloadMatchesGetBrandFromOmeEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GetBrandFromOmeJobDispatchPublisherImpl publisher = new GetBrandFromOmeJobDispatchPublisherImpl(dispatchFacade);

		long companyId = 55L;
		long endUnix = 1_735_689_600L;
		publisher.enqueueGetBrandFromOme(companyId, 1, endUnix);

		verify(dispatchFacade)
				.dispatchJob(
						eq(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME),
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
									return map.size() == 3;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	@Test
	void whenNotLastPage_enqueuesNextPageJob() {
		long companyId = 13L;
		int page = 1;
		long endUnix = 1_700_030_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getBrandCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> brandRow = new LinkedHashMap<>();
		brandRow.put("id", "b1");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("rsp", "succ");
		data.put("count", 25);
		data.put("lists", List.of(brandRow));
		Map<String, Object> apiWrap = new LinkedHashMap<>();
		apiWrap.put("rsp", "succ");
		apiWrap.put("data", data);

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goodsbrand.getList"), any())).thenReturn(apiWrap);

		OmeBrandBatchPersistService persist = mock(OmeBrandBatchPersistService.class);
		ObjectMapper om = new ObjectMapper();

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		GetBrandFromOmeJobDispatchPublisherImpl publisher = new GetBrandFromOmeJobDispatchPublisherImpl(facade);
		OmeBrandFromOmePagedSyncRunner runner =
				new OmeBrandFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, publisher);
		GetBrandFromOmeJobHandler handler = new GetBrandFromOmeJobHandler(runner);
		registry.registerJob(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME,
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
		assertEquals(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, second.messageName());

		Map<String, Object> p2 = second.payload();
		assertEquals(companyId, asLong(p2.get("company_id")));
		assertEquals(2, asInt(p2.get("page")));
		assertEquals(endUnix, asLong(p2.get("end_lastmodify_unix")));

		verify(lastTime, never()).setBrandCursorUnix(anyLong(), anyLong());
		verify(persist, times(1)).saveBrands(eq(companyId), any());
	}

	/**
	 * Second hop on the bus: consume {@code page=2} with {@code count=25} so the runner enqueues {@code page=3} on
	 * {@code slow} with identical metadata to the first message; cursor must not advance until the final page.
	 * Complements {@link #whenNotLastPage_enqueuesNextPageJob()}. Anchor {@code entry-02-jc-getbrandfromome-handle}.
	 */
	@Test
	void dispatchJob_afterPageTwoConsumes_enqueuesPageThreeOnSlowQueue() {
		long companyId = 17L;
		int page = 2;
		long endUnix = 1_700_050_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getBrandCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> brandRow = new LinkedHashMap<>();
		brandRow.put("id", "b2");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("count", 25);
		data.put("lists", List.of(brandRow));
		Map<String, Object> apiWrap = new LinkedHashMap<>();
		apiWrap.put("rsp", "succ");
		apiWrap.put("data", data);

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goodsbrand.getList"), any())).thenReturn(apiWrap);

		OmeBrandBatchPersistService persist = mock(OmeBrandBatchPersistService.class);
		ObjectMapper om = new ObjectMapper();

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		GetBrandFromOmeJobDispatchPublisherImpl publisher = new GetBrandFromOmeJobDispatchPublisherImpl(facade);
		OmeBrandFromOmePagedSyncRunner runner =
				new OmeBrandFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, publisher);
		GetBrandFromOmeJobHandler handler = new GetBrandFromOmeJobHandler(runner);
		registry.registerJob(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME,
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
		assertEquals(first.messageType(), second.messageType());
		assertEquals(first.dispatchMode(), second.dispatchMode());
		assertEquals(first.driverType(), second.driverType());
		assertEquals(first.queue(), second.queue());
		assertEquals(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, second.messageName());

		Map<String, Object> p2 = second.payload();
		assertEquals(companyId, asLong(p2.get("company_id")));
		assertEquals(3, asInt(p2.get("page")));
		assertEquals(endUnix, asLong(p2.get("end_lastmodify_unix")));

		verify(lastTime, never()).setBrandCursorUnix(anyLong(), anyLong());
		verify(persist, times(1)).saveBrands(eq(companyId), any());
	}

	@Test
	void whenLastPage_doesNotEnqueueNext_andSetsBrandCursor() {
		long companyId = 14L;
		int page = 1;
		long endUnix = 1_700_040_000L;
		long startUnix = 1_700_000_000L;

		ShopexErpSettingRedisAccessor settings = mock(ShopexErpSettingRedisAccessor.class);
		when(settings.getParsedSetting(companyId)).thenReturn(Map.of("is_openapi_open", true));

		OmeLastTimeRedisAccessor lastTime = mock(OmeLastTimeRedisAccessor.class);
		when(lastTime.getBrandCursorUnix(companyId)).thenReturn(startUnix);

		Map<String, Object> brandRow = new LinkedHashMap<>();
		brandRow.put("id", "b-last");
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("count", 10);
		data.put("lists", List.of(brandRow));
		Map<String, Object> apiWrap = new LinkedHashMap<>();
		apiWrap.put("rsp", "succ");
		apiWrap.put("data", data);

		ShopexErpOpenApiClient openApi = mock(ShopexErpOpenApiClient.class);
		when(openApi.call(eq(companyId), eq("goodsbrand.getList"), any())).thenReturn(apiWrap);

		OmeBrandBatchPersistService persist = mock(OmeBrandBatchPersistService.class);
		ObjectMapper om = new ObjectMapper();

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		GetBrandFromOmeJobDispatchPublisherImpl publisher = new GetBrandFromOmeJobDispatchPublisherImpl(facade);
		OmeBrandFromOmePagedSyncRunner runner =
				new OmeBrandFromOmePagedSyncRunner(settings, lastTime, openApi, persist, om, publisher);
		GetBrandFromOmeJobHandler handler = new GetBrandFromOmeJobHandler(runner);
		registry.registerJob(GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME, handler);

		assertEquals(
				false,
				OmeBrandFromOmePagedSyncRunner.shouldEnqueueNextPage(page, 10, 10),
				"page 1 of 10 total rows is the last page at PAGE_SIZE=10");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("page", page);
		payload.put("end_lastmodify_unix", endUnix);

		facade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_BRAND_FROM_OME,
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

		assertEquals(1, captured.size(), "last page must not enqueue a follow-up job");
		verify(lastTime, times(1)).setBrandCursorUnix(eq(companyId), eq(endUnix));
		verify(persist, times(1)).saveBrands(eq(companyId), any());
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
