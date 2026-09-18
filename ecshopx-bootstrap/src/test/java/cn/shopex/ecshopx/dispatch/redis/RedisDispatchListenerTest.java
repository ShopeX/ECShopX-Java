package cn.shopex.ecshopx.dispatch.redis;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisDispatchListenerTest {

    @Test
    void pollDecodesAndDeliversMessage() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(objectMapper);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(listOperations);
        DispatchConsumerRuntime runtime = mock(DispatchConsumerRuntime.class);
        RedisDispatchListener listener = new RedisDispatchListener(redis, codec, runtime, "dispatch:");

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
        when(listOperations.leftPop("dispatch:default")).thenReturn(codec.encode(message));

        listener.poll();

        verify(runtime).consume(ArgumentMatchers.argThat(actual ->
                actual.messageName().equals("event:member_created")
                        && actual.queue().equals("sms")
                        && actual.traceId().equals("trace-1")
                        && actual.listenerName().equals("listener:sms")
                        && actual.driverType() == DispatchDriverType.REDIS),
                ArgumentMatchers.eq(1));
        assertNotNull(listener);
    }

    @Test
    void pollAlsoConsumesSmsQueue() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(objectMapper);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        when(redis.opsForList()).thenReturn(listOperations);
        DispatchConsumerRuntime runtime = mock(DispatchConsumerRuntime.class);
        RedisDispatchListener listener = new RedisDispatchListener(redis, codec, runtime, "dispatch:");

        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "job:51:AliyunsmsBundle\\Jobs\\QuerySmsSign",
                Map.of("company_id", 7L, "sign_name", "StoreSign"),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-08-12T00:00:00Z"),
                "trace-sms",
                null);
        when(listOperations.leftPop("dispatch:default")).thenReturn(null);
        when(listOperations.leftPop("dispatch:slow")).thenReturn(null);
        when(listOperations.leftPop("dispatch:sms")).thenReturn(codec.encode(message)).thenReturn(null);

        listener.poll();

        verify(runtime).consume(ArgumentMatchers.argThat(actual ->
                actual.messageName().equals("job:51:AliyunsmsBundle\\Jobs\\QuerySmsSign")
                        && actual.queue().equals("sms")
                        && actual.traceId().equals("trace-sms")
                        && actual.driverType() == DispatchDriverType.REDIS),
                ArgumentMatchers.eq(1));
    }
}
