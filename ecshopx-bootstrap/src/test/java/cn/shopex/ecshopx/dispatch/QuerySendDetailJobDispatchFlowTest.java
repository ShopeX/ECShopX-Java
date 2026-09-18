package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.dispatch.QuerySendDetailJobHandler;
import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySendDetailsClient;
import cn.shopex.ecshopx.common.aliyunsms.QuerySendDetailsResult;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import cn.shopex.ecshopx.config.AliyunsmsQuerySendDetailJobDispatchPublisherImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuerySendDetailJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerUpdatesRecord() {
		RecordMapper recordMapper = mock(RecordMapper.class);
		AliyunsmsQuerySendDetailsClient client = mock(AliyunsmsQuerySendDetailsClient.class);
		when(client.querySendDetails(eq(10L), eq("13800138000"), eq("BIZ1"), eq("20250101")))
				.thenReturn(new QuerySendDetailsResult("3", "body"));
		Record current = new Record();
		current.setId(7L);
		when(recordMapper.selectOne(any(QueryWrapper.class))).thenReturn(current);
		when(recordMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

		QuerySendDetailJobHandler handler = new QuerySendDetailJobHandler(recordMapper, client);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("id", 7L);
		payload.put("mobile", "13800138000");
		payload.put("biz_id", "BIZ1");
		payload.put("created", 1_735_660_800);

		facade.dispatchJob(
				AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(client).querySendDetails(eq(10L), eq("13800138000"), eq("BIZ1"), eq("20250101"));
		verify(recordMapper).selectOne(any(QueryWrapper.class));
		verify(recordMapper).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	void dispatchJob_async_payloadMatchesQuerySendDetailPublisherEnvelope() {
		RecordMapper recordMapper = mock(RecordMapper.class);
		AliyunsmsQuerySendDetailsClient client = mock(AliyunsmsQuerySendDetailsClient.class);
		QuerySendDetailJobHandler handler = new QuerySendDetailJobHandler(recordMapper, client);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		AliyunsmsQuerySendDetailJobDispatchPublisherImpl publisher =
				new AliyunsmsQuerySendDetailJobDispatchPublisherImpl(facade);
		publisher.publish(99L, 5L, "13900139000", "BIZ-9", 1_800_000_100);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB, msg.messageName());
		Map<String, Object> p = msg.payload();
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("company_id", 99L);
		expected.put("id", 5L);
		expected.put("mobile", "13900139000");
		expected.put("biz_id", "BIZ-9");
		expected.put("created", 1_800_000_100);
		assertEquals(expected, p);
		assertEquals(5, p.size());
	}

	@Test
	void runtimeConsume_handCraftedQuerySendDetailJob_invokesClientAndMapper() {
		RecordMapper recordMapper = mock(RecordMapper.class);
		AliyunsmsQuerySendDetailsClient client = mock(AliyunsmsQuerySendDetailsClient.class);
		when(client.querySendDetails(anyLong(), anyString(), anyString(), anyString()))
				.thenReturn(QuerySendDetailsResult.noDetail());
		QuerySendDetailJobHandler handler = new QuerySendDetailJobHandler(recordMapper, client);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("id", 2L);
		payload.put("mobile", "13000000001");
		payload.put("biz_id", "BZ");
		payload.put("created", 1_800_000_200);

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						AliyunsmsDispatchJobNames.QUERY_SEND_DETAIL_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-query-send-detail-consumer",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<String> sendDateCap = ArgumentCaptor.forClass(String.class);
		verify(client).querySendDetails(eq(1L), eq("13000000001"), eq("BZ"), sendDateCap.capture());
		assertEquals("20270115", sendDateCap.getValue());
		verify(recordMapper, never()).selectOne(any());
		verify(recordMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}
}
