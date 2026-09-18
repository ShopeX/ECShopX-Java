package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.dispatch.rabbit.RabbitDispatchListener;
import cn.shopex.ecshopx.dispatch.rabbit.RabbitDispatchMessageConverter;
import cn.shopex.ecshopx.dispatch.rabbit.RabbitDispatchDriver;
import cn.shopex.ecshopx.dispatch.redis.RedisDispatchListener;
import cn.shopex.ecshopx.dispatch.redis.RedisDispatchMessageCodec;
import cn.shopex.ecshopx.dispatch.redis.RedisDispatchDriver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class DispatchBusIntegrationTest {

    @Test
    void syncJobAndAsyncEventFanOutAreDispatched() {
        DispatchRegistry registry = new InMemoryDispatchRegistry();
        DispatchHandler jobHandler = mock(DispatchHandler.class);
        DispatchListener eventListener = mock(DispatchListener.class);
        registry.registerJob("job:sync", jobHandler);
        registry.registerEventListener("event:sync", "listener:sms", ListenerDispatchOptions.asyncDefaults(), eventListener);

        SyncDispatchDriver syncDriver = new SyncDispatchDriver(registry);
        syncDriver.execute(new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.SYNC,
                DispatchDriverType.SYNC,
                "job:sync",
                Map.of("id", 1L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-sync-job",
                null));

        DispatchCore core = DispatchCore.asyncReady(registry, syncDriver, Map.of(DispatchDriverType.REDIS, message -> syncDriver.execute(message)));
        DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

        facade.publishEvent("event:sync", Map.of("id", 2L), DispatchOptions.eventDefaults());

        verify(jobHandler).handle(Map.of("id", 1L));
        verify(eventListener).onEvent(Map.of("id", 2L));
        assertNotNull(facade.lastPublishedMessage());
        assertEquals("event:sync", facade.lastPublishedMessage().messageName());
    }

    @Test
    void redisListenerConsumesAndDispatches() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 9L),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        DispatchConsumerRuntime runtime = mock(DispatchConsumerRuntime.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(listOperations);
        RedisDispatchMessageCodec redisCodec = new RedisDispatchMessageCodec(objectMapper);
        RedisDispatchListener redisListener = new RedisDispatchListener(redis, redisCodec, runtime, "dispatch:");
        when(listOperations.leftPop("dispatch:default")).thenReturn(redisCodec.encode(message));

        redisListener.poll();

        verify(runtime).consume(ArgumentMatchers.argThat(actual -> actual.messageName().equals("event:member_created")), ArgumentMatchers.eq(1));
    }

    @Test
    void rabbitListenerConsumesAndDispatches() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.RABBITMQ,
                "event:member_created",
                Map.of("memberId", 9L),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        DispatchConsumerRuntime runtime = mock(DispatchConsumerRuntime.class);
        RabbitDispatchMessageConverter rabbitConverter = new RabbitDispatchMessageConverter(objectMapper);
        RabbitDispatchListener rabbitListener = new RabbitDispatchListener(rabbitConverter, runtime);

        rabbitListener.onMessage(rabbitConverter.toBytes(message));

        verify(runtime).consume(ArgumentMatchers.argThat(actual -> actual.messageName().equals("event:member_created")), ArgumentMatchers.eq(1));
        assertNotNull(rabbitListener);
    }
}

