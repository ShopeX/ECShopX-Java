package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.promotions.dispatch.CancelSeckillPlatTicketJobHandler;
import cn.shopex.ecshopx.promotions.service.wxapp.SeckillTicketHashidsSupport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class CancelSeckillPlatTicketJobDispatchFlowTest {

	@Test
	@DisplayName("dispatchJob: async delayed job targets seckill queue with 300s delay and full payload")
	void dispatchJob_delayedAsync_enqueuesSeckillQueue_andMessageMatchesJobNameAndPayload() {
		CancelSeckillPlatTicketJobHandler handler =
				new CancelSeckillPlatTicketJobHandler(mock(StringRedisTemplate.class), new SeckillTicketHashidsSupport());

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("ticketkey", "tk1");
		payload.put("seckillkey", "sk1");
		payload.put("productkey", "store_9");
		payload.put("num", 2);
		payload.put("userId", "42");

		facade.dispatchJob(
				PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"seckill",
						Duration.ofSeconds(300),
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("seckill", msg.queue());
		assertEquals(Duration.ofSeconds(300), msg.delay());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET, msg.messageName());
		assertEquals("tk1", msg.payload().get("ticketkey"));
		assertEquals("sk1", msg.payload().get("seckillkey"));
		assertEquals("store_9", msg.payload().get("productkey"));
		assertEquals(2, msg.payload().get("num"));
		assertEquals("42", msg.payload().get("userId"));
	}

	@Test
	@DisplayName("consumer: handler deletes ticket and restores stock when decoded quantity matches payload num")
	void consumer_invokesHandler_restoresStockWhenTicketNumMatches() {
		SeckillTicketHashidsSupport hashids = new SeckillTicketHashidsSupport();
		String encodedTicket = hashids.encode(new long[] {3, 100, 200, 1_700_000_000});

		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.get("ticket-key-1", "99")).thenReturn(encodedTicket);
		when(hashOps.delete("ticket-key-1", "99")).thenReturn(1L);

		CancelSeckillPlatTicketJobHandler handler = new CancelSeckillPlatTicketJobHandler(redis, hashids);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("ticketkey", "ticket-key-1");
		payload.put("seckillkey", "seckill-key-1");
		payload.put("productkey", "store_200");
		payload.put("num", 3);
		payload.put("userId", "99");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET,
						payload,
						"seckill",
						Duration.ofSeconds(300),
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						"trace",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(hashOps).delete("ticket-key-1", "99");
		verify(hashOps).increment("seckill-key-1", "store_200", 3L);
	}

	@Test
	@DisplayName("consumer: handler does not restore stock when ticket quantity does not match payload num")
	void consumer_handlerDoesNotIncrementStockWhenDecodedNumMismatches() {
		SeckillTicketHashidsSupport hashids = new SeckillTicketHashidsSupport();
		String encodedTicket = hashids.encode(new long[] {5, 100, 200, 1_700_000_000});

		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hashOps);
		when(hashOps.get(anyString(), anyString())).thenReturn(encodedTicket);

		CancelSeckillPlatTicketJobHandler handler = new CancelSeckillPlatTicketJobHandler(redis, hashids);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET, handler);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("ticketkey", "ticket-key-1");
		payload.put("seckillkey", "seckill-key-1");
		payload.put("productkey", "store_200");
		payload.put("num", 3);
		payload.put("userId", "99");

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET,
						payload,
						"seckill",
						Duration.ofSeconds(300),
						RetryPolicy.platformDefault(),
						java.time.Instant.now(),
						"trace",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(hashOps, never()).delete(anyString(), anyString());
		verify(hashOps, never()).increment(anyString(), anyString(), anyLong());
	}
}
