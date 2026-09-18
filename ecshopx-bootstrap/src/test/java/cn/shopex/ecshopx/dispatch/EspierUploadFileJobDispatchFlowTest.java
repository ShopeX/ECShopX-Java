package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierUploadFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.espier.dispatch.EspierUploadFileJobHandler;
import cn.shopex.ecshopx.espier.service.EspierUploadFileAsyncRunner;
import cn.shopex.ecshopx.espier.service.UploadeFileRepository;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import cn.shopex.ecshopx.espier.service.upload.ImportDataJobDispatchPort;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EspierUploadFileJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues slow queue without delay and consumer invokes runAfterCommit with payload")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesRunAfterCommit() {
		EspierUploadFileAsyncRunner asyncRunner = Mockito.mock(EspierUploadFileAsyncRunner.class);
		EspierUploadFileJobHandler handler = new EspierUploadFileJobHandler(asyncRunner);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.UPLOAD_FILE_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", 42L);
		payload.put("company_id", 1L);
		payload.put("operator_id", 2L);
		payload.put("supplier_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("merchant_id", 0L);
		payload.put("file_type", "member_info");
		payload.put("storage_path", "imports/member_info/1/probe.xlsx");
		payload.put("request_file_type", "member_info");
		payload.put("operator_type", "admin");

		facade.dispatchJob(
				EspierDispatchJobNames.UPLOAD_FILE_JOB,
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
		assertEquals(EspierDispatchJobNames.UPLOAD_FILE_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertFalse(got.containsKey("file_name"));
		assertFalse(got.containsKey("file_size"));
		assertFalse(got.containsKey("handle_status"));
		assertFalse(got.containsKey("handle_line_num"));
		assertFalse(got.containsKey("created"));
		assertFalse(got.containsKey("left_job_num"));
		assertEquals(42L, asLong(got.get("id")));
		assertEquals(1L, asLong(got.get("company_id")));
		assertEquals(2L, asLong(got.get("operator_id")));
		assertEquals("imports/member_info/1/probe.xlsx", got.get("storage_path"));
		assertEquals("admin", got.get("operator_type"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(asyncRunner)
				.runAfterCommit(
						argThat(
								p ->
										p.getId() == 42L
												&& p.getCompanyId() == 1L
												&& p.getOperatorId() == 2L
												&& p.getSupplierId() == 0L
												&& p.getDistributorId() == 0L
												&& p.getMerchantId() == 0L
												&& "member_info".equals(p.getFileType())
												&& "imports/member_info/1/probe.xlsx".equals(p.getStoragePath())
												&& "member_info".equals(p.getRequestFileType())
												&& "admin".equals(p.getOperatorType())));
	}

	@Test
	@DisplayName("consume drives handler that reloads upload row by id before early return when status not wait")
	void consume_invokesRunAfterCommitWhichReloadsUploadeFileRowById() {
		UploadeFileRepository uploadeFileRepository = mock(UploadeFileRepository.class);
		EspierUploadFileHandlerRegistry handlerRegistry = mock(EspierUploadFileHandlerRegistry.class);
		FileStorageService fileStorageService = mock(FileStorageService.class);
		EspierUploadFileAsyncRunner runner =
				new EspierUploadFileAsyncRunner(
						uploadeFileRepository,
						handlerRegistry,
						fileStorageService,
						new ObjectMapper(),
						mock(ImportDataJobDispatchPort.class));
		EspierUploadFileJobHandler handler = new EspierUploadFileJobHandler(runner);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.UPLOAD_FILE_JOB, handler);

		LinkedHashMap<String, Object> waitRow = new LinkedHashMap<>();
		waitRow.put("id", 42L);
		waitRow.put("handle_status", "wait");
		waitRow.put("file_name", "wide.xlsx");
		waitRow.put("file_size", "1024");
		waitRow.put("created", 1700000000);
		waitRow.put("left_job_num", 1);

		LinkedHashMap<String, Object> notWaitRow = new LinkedHashMap<>(waitRow);
		notWaitRow.put("handle_status", "finish");

		when(uploadeFileRepository.getInfoById(42L)).thenReturn(waitRow).thenReturn(notWaitRow);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", 42L);
		payload.put("company_id", 1L);
		payload.put("operator_id", 2L);
		payload.put("supplier_id", 0L);
		payload.put("distributor_id", 0L);
		payload.put("merchant_id", 0L);
		payload.put("file_type", "member_info");
		payload.put("storage_path", "path/wide.xlsx");
		payload.put("request_file_type", "member_info");
		payload.put("operator_type", "admin");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						EspierDispatchJobNames.UPLOAD_FILE_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						UUID.randomUUID().toString(),
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(uploadeFileRepository, atLeastOnce()).getInfoById(42L);
		verify(uploadeFileRepository, never()).updateOneBy(any(), any());
		verify(fileStorageService, never()).get(any(), any());
	}

	@Test
	void dispatchJob_publishPayloadMatchesEspierUploadFileJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		EspierUploadFileJobDispatchPublisherImpl publisher = new EspierUploadFileJobDispatchPublisherImpl(dispatchFacade);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("id", 42L);
		result.put("company_id", 100L);
		result.put("operator_id", 200L);
		result.put("supplier_id", 0L);
		result.put("distributor_id", 1L);
		result.put("merchant_id", 2L);
		result.put("file_type", "member_info");
		result.put("operator_type", "admin");
		EspierUploadFileQueuedPayload payload =
				EspierUploadFileQueuedPayload.fromPersistedRow(result, "rel/path.xlsx", "member_info");

		publisher.publishAfterCommit(payload);

		verify(dispatchFacade)
				.dispatchJob(
						eq(EspierDispatchJobNames.UPLOAD_FILE_JOB),
						argThat(
								m -> {
									if (42L != asLong(m.get("id"))) {
										return false;
									}
									if (100L != asLong(m.get("company_id")) || 200L != asLong(m.get("operator_id"))) {
										return false;
									}
									if (0L != asLong(m.get("supplier_id"))
											|| 1L != asLong(m.get("distributor_id"))
											|| 2L != asLong(m.get("merchant_id"))) {
										return false;
									}
									if (!"member_info".equals(m.get("file_type"))) {
										return false;
									}
									if (!"rel/path.xlsx".equals(m.get("storage_path"))) {
										return false;
									}
									if (!"member_info".equals(m.get("request_file_type"))) {
										return false;
									}
									if (!"admin".equals(m.get("operator_type"))) {
										return false;
									}
									return m.size() == 10;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
