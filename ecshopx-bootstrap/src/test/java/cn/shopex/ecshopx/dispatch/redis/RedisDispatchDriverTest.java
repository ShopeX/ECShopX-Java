package cn.shopex.ecshopx.dispatch.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class RedisDispatchDriverTest {

    @Test
    void enqueueUsesDefaultQueueWhenJobQueueMissing() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ListOperations<String, String> ops = Mockito.mock(ListOperations.class);
        Mockito.when(redis.opsForList()).thenReturn(ops);
        RedisDispatchDriver driver = new RedisDispatchDriver(
                redis,
                new RedisDispatchMessageCodec(new ObjectMapper().registerModule(new JavaTimeModule())),
                "dispatch:ready:");

        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "job:test",
                Map.of("id", 1),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                null);

        driver.enqueue(message);
        verify(ops).rightPush(Mockito.eq("dispatch:ready:default"), Mockito.anyString());
    }

    @Test
    void enqueueWithDelayUsesDelayedZSetInsteadOfReadyList() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOps = Mockito.mock(ListOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zsetOps = Mockito.mock(ZSetOperations.class);
        Mockito.when(redis.opsForList()).thenReturn(listOps);
        Mockito.when(redis.opsForZSet()).thenReturn(zsetOps);
        RedisDispatchMessageCodec codec =
                new RedisDispatchMessageCodec(new ObjectMapper().registerModule(new JavaTimeModule()));
        RedisDispatchDriver driver =
                new RedisDispatchDriver(redis, codec, "dispatch:ready:", null, "dispatch:test-delayed");

        long beforeMs = System.currentTimeMillis();
        DispatchMessage message =
                new DispatchMessage(
                        DispatchMessageType.JOB,
                        DispatchMode.ASYNC,
                        DispatchDriverType.REDIS,
                        "job:delayed",
                        Map.of("id", 1),
                        "slow",
                        Duration.ofSeconds(61),
                        RetryPolicy.platformDefault(),
                        Instant.parse("2026-05-02T00:00:00Z"),
                        "trace-delay",
                        null);
        driver.enqueue(message);
        long afterMs = System.currentTimeMillis();

        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zsetOps).add(eq("dispatch:test-delayed"), anyString(), scoreCaptor.capture());
        double score = scoreCaptor.getValue();
        assertThat(score).isBetween((double) (beforeMs + 61_000 - 2_000), (double) (afterMs + 61_000 + 2_000));
        verify(listOps, never()).rightPush(anyString(), anyString());
    }

    @Test
    void ackDoesNotRequeueMessage() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ListOperations<String, String> ops = Mockito.mock(ListOperations.class);
        Mockito.when(redis.opsForList()).thenReturn(ops);
        RedisDispatchDriver driver = new RedisDispatchDriver(redis, new RedisDispatchMessageCodec(new ObjectMapper().registerModule(new JavaTimeModule())), "dispatch:ready:");
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:test",
                Map.of("id", 2),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-2",
                "listener:sms");

        driver.ack(message);

        Mockito.verifyNoInteractions(ops);
    }

    @Test
    void retryRequeuesMessageToReadyQueue() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ListOperations<String, String> ops = Mockito.mock(ListOperations.class);
        Mockito.when(redis.opsForList()).thenReturn(ops);
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(new ObjectMapper().registerModule(new JavaTimeModule()));
        RedisDispatchDriver driver = new RedisDispatchDriver(redis, codec, "dispatch:ready:");
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:test",
                Map.of("id", 2),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-2",
                "listener:sms");

        driver.retry(message);

        verify(ops).rightPush("dispatch:ready:sms", codec.encode(message));
    }

    @Test
    void failRoutesMessageToDeadQueue() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ListOperations<String, String> ops = Mockito.mock(ListOperations.class);
        Mockito.when(redis.opsForList()).thenReturn(ops);
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(new ObjectMapper().registerModule(new JavaTimeModule()));
        RedisDispatchDriver driver = new RedisDispatchDriver(redis, codec, "dispatch:ready:");
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:test",
                Map.of("id", 3),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-3",
                "listener:sms");

        driver.fail(message);

        verify(ops).rightPush("dispatch:ready:dead:sms", codec.encode(message));
    }
}
