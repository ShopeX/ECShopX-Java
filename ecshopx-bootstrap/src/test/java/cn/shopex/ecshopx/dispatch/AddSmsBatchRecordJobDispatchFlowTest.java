package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.dispatch.AddSmsBatchRecordJobHandler;
import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import cn.shopex.ecshopx.config.AliyunsmsAddSmsBatchRecordJobDispatchPublisherImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AddSmsBatchRecordJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSmsQueue_andConsumerInvokesRecordMapper() {
		RecordMapper recordMapper = mock(RecordMapper.class);
		SensitiveFieldEncryptor encryptor = mock(SensitiveFieldEncryptor.class);
		when(encryptor.encrypt(any())).thenAnswer(inv -> "enc:" + inv.getArgument(0, String.class));
		AddSmsBatchRecordJobHandler handler = new AddSmsBatchRecordJobHandler(recordMapper, encryptor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("task_id", 7);
		payload.put("mobile", new ArrayList<>(List.of("13000000001", "13000000002")));
		payload.put("scene_id", 99);
		payload.put("template_code", "TCODE");
		payload.put("template_type", "2");
		payload.put("sms_content", "【S】hello");
		payload.put("status", 1);
		payload.put("biz_id", "BIZ-1");

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"sms",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("sms", msg.queue());
		assertEquals(AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Record> recCap = ArgumentCaptor.forClass(Record.class);
		verify(recordMapper, times(2)).insert(recCap.capture());
		List<Record> rows = recCap.getAllValues();
		assertEquals(100L, rows.get(0).getCompanyId());
		assertEquals(7, rows.get(0).getTaskId());
		assertEquals(99, rows.get(0).getSceneId());
		assertEquals("TCODE", rows.get(0).getTemplateCode());
		assertEquals("2", rows.get(0).getTemplateType());
		assertEquals("【S】hello", rows.get(0).getSmsContent());
		assertEquals("1", rows.get(0).getStatus());
		assertEquals("BIZ-1", rows.get(0).getBizId());
		assertEquals("enc:13000000001", rows.get(0).getMobile());
		assertEquals("enc:13000000002", rows.get(1).getMobile());
		verify(encryptor).encrypt(eq("13000000001"));
		verify(encryptor).encrypt(eq("13000000002"));
	}

	@Test
	void dispatchJob_async_payloadMatchesAddBatchRecordPublisherEnvelope() {
		RecordMapper recordMapper = mock(RecordMapper.class);
		SensitiveFieldEncryptor encryptor = mock(SensitiveFieldEncryptor.class);
		when(encryptor.encrypt(any())).thenAnswer(inv -> "E_" + inv.getArgument(0, String.class));
		AddSmsBatchRecordJobHandler handler = new AddSmsBatchRecordJobHandler(recordMapper, encryptor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		AliyunsmsAddSmsBatchRecordJobDispatchPublisherImpl publisher =
				new AliyunsmsAddSmsBatchRecordJobDispatchPublisherImpl(facade);

		List<String> mobiles = List.of("13800138000", "13900139000");
		publisher.publish(99L, 5, mobiles, 42, "CODE", "2", "【签】内容", 1, "BIZ-9");

		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("company_id", 99L);
		expected.put("task_id", 5);
		expected.put("mobile", new ArrayList<>(mobiles));
		expected.put("scene_id", 42);
		expected.put("template_code", "CODE");
		expected.put("template_type", "2");
		expected.put("sms_content", "【签】内容");
		expected.put("status", 1);
		expected.put("biz_id", "BIZ-9");

		assertEquals(1, captured.size());
		assertEquals(expected, captured.get(0).payload());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);

		verify(recordMapper, times(2)).insert(any(Record.class));
	}

	@Test
	void runtimeConsume_handCraftedAddBatchRecordJob_insertsEncryptedRows() {
		RecordMapper recordMapper = mock(RecordMapper.class);
		SensitiveFieldEncryptor encryptor = mock(SensitiveFieldEncryptor.class);
		when(encryptor.encrypt(any())).thenAnswer(inv -> "X_" + inv.getArgument(0, String.class));
		AddSmsBatchRecordJobHandler handler = new AddSmsBatchRecordJobHandler(recordMapper, encryptor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("task_id", 2);
		payload.put("mobile", new ArrayList<>(List.of("15500001111")));
		payload.put("scene_id", 3);
		payload.put("template_code", "T");
		payload.put("template_type", "0");
		payload.put("sms_content", "body");
		payload.put("status", 1);
		payload.put("biz_id", "Z");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AliyunsmsDispatchJobNames.ADD_SMS_BATCH_RECORD_JOB,
						payload,
						"sms",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-add-batch-record-consumer",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<Record> cap = ArgumentCaptor.forClass(Record.class);
		verify(recordMapper).insert(cap.capture());
		Record r = cap.getValue();
		assertEquals(1L, r.getCompanyId());
		assertEquals("X_15500001111", r.getMobile());
		assertEquals("1", r.getStatus());
		assertEquals("Z", r.getBizId());
	}
}
