package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.ImportDataJobDispatchPublisherImpl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ImportDataJobDispatchFlowTest {

	@Test
	@DisplayName("SYNC import-data dispatches inline with descending sort; REDIS capture stays empty")
	void dispatchJob_sync_executesHandlerInline_sortDescendingAcrossTwoChunks() {
		DispatchHandler handler = mock(DispatchHandler.class);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.IMPORT_DATA_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		ImportDataJobDispatchPublisherImpl publisher = new ImportDataJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> uploadInfo = new LinkedHashMap<>();
		uploadInfo.put("id", 1L);
		uploadInfo.put("company_id", 10L);
		uploadInfo.put("storage_path", "p/a.xlsx");

		LinkedHashMap<Integer, String> column = new LinkedHashMap<>();
		column.put(0, "c0");

		List<Map<String, Object>> chunk1 = List.of(Map.of("c0", "a"));
		List<Map<String, Object>> chunk2 = List.of(Map.of("c0", "b"));

		publisher.dispatchImportDataChunk(true, uploadInfo, chunk1, column, 2, List.of("H"));
		publisher.dispatchImportDataChunk(true, uploadInfo, chunk2, column, 1, List.of("H"));

		assertTrue(captured.isEmpty(), "SYNC must not enqueue async REDIS driver");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(handler, times(2)).handle(cap.capture());
		List<Map<String, Object>> payloads = cap.getAllValues();
		assertEquals(2, ((Number) payloads.get(0).get("sort")).intValue());
		assertEquals(1, ((Number) payloads.get(1).get("sort")).intValue());
	}

	@Test
	@DisplayName("ASYNC import-data enqueues on slow REDIS queue; consumer invokes handler once")
	void dispatchJob_async_enqueuesOnSlowQueue_redisCaptureNonEmpty() {
		DispatchHandler handler = mock(DispatchHandler.class);
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.IMPORT_DATA_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		ImportDataJobDispatchPublisherImpl publisher = new ImportDataJobDispatchPublisherImpl(facade);

		LinkedHashMap<String, Object> uploadInfo = new LinkedHashMap<>();
		uploadInfo.put("id", 7L);
		uploadInfo.put("company_id", 1L);
		LinkedHashMap<Integer, String> column = new LinkedHashMap<>();
		column.put(0, "c0");
		List<Map<String, Object>> chunk = List.of(Map.of("c0", "x"));

		publisher.dispatchImportDataChunk(false, uploadInfo, chunk, column, 1, List.of("H"));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.IMPORT_DATA_JOB, msg.messageName());
		assertNull(msg.listenerName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(handler, times(1)).handle(cap.capture());
		assertEquals(1, ((Number) cap.getValue().get("sort")).intValue());
	}

	@Test
	@DisplayName("ImportData publisher builds envelope and SYNC options for dispatchFacade")
	void dispatchJob_publishPayloadMatchesImportDataEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		ImportDataJobDispatchPublisherImpl publisher = new ImportDataJobDispatchPublisherImpl(dispatchFacade);

		LinkedHashMap<String, Object> uploadInfo = new LinkedHashMap<>();
		uploadInfo.put("id", 42L);
		uploadInfo.put("company_id", 100L);
		uploadInfo.put("file_type", "member_info");

		LinkedHashMap<Integer, String> column = new LinkedHashMap<>();
		column.put(0, "col_a");
		column.put(1, "col_b");

		List<Map<String, Object>> params = new ArrayList<>();
		params.add(Map.of("col_a", "x", "col_b", "y"));

		publisher.dispatchImportDataChunk(true, uploadInfo, params, column, 3, List.of("A", "B"));

		verify(dispatchFacade)
				.dispatchJob(
						eq(EspierDispatchJobNames.IMPORT_DATA_JOB),
						argThat(
								m -> {
									if (!"member_info".equals(((Map<?, ?>) m.get("upload_file_info")).get("file_type"))) {
										return false;
									}
									@SuppressWarnings("unchecked")
									List<Map<String, Object>> ps = (List<Map<String, Object>>) m.get("params");
									if (ps == null || ps.size() != 1) {
										return false;
									}
									@SuppressWarnings("unchecked")
									Map<String, String> col = (Map<String, String>) m.get("column");
									if (!"col_a".equals(col.get("0")) || !"col_b".equals(col.get("1"))) {
										return false;
									}
									if (((Number) m.get("sort")).intValue() != 3) {
										return false;
									}
									@SuppressWarnings("unchecked")
									List<String> ex = (List<String>) m.get("export_header_title_columns");
									return ex != null && ex.size() == 2 && "A".equals(ex.get(0)) && "B".equals(ex.get(1));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.SYNC
												&& opts.queue() == null
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}
}
