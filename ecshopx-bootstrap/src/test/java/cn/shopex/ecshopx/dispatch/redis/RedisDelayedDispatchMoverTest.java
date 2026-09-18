package cn.shopex.ecshopx.dispatch.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class RedisDelayedDispatchMoverTest {

    @Test
    void moveReadyMessagesPushesExpiredPayloadsToReadyQueueAndRemovesDelayedEntries() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);
        Mockito.when(redis.opsForZSet()).thenReturn(zset);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(objectMapper);
        DispatchMessage delayedMessage = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:test",
                java.util.Map.of("id", 1),
                "sms",
                Duration.ofSeconds(5),
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "smsListener");
        String encoded = codec.encode(delayedMessage);
        Mockito.when(zset.rangeByScore("dispatch:delayed", 0, 1000)).thenReturn(Set.of(encoded));
        RedisDispatchDriver readyDriver = Mockito.mock(RedisDispatchDriver.class);
        RedisDelayedDispatchMover mover = new RedisDelayedDispatchMover(redis, codec, readyDriver, "dispatch:delayed");

        mover.moveReadyMessages(Instant.ofEpochMilli(1000));

        verify(readyDriver)
                .enqueue(
                        argThat(
                                m ->
                                        m.messageName().equals(delayedMessage.messageName())
                                                && m.delay() == null
                                                && m.queue().equals(delayedMessage.queue())));
        verify(zset).remove("dispatch:delayed", encoded);
    }

    @Test
    void moveReadyMessagesLeavesRedisUntouchedWhenNoEntriesAreReady() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);
        Mockito.when(redis.opsForZSet()).thenReturn(zset);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(objectMapper);
        Mockito.when(zset.rangeByScore("dispatch:delayed", 0, 1000)).thenReturn(Set.of());
        RedisDispatchDriver readyDriver = Mockito.mock(RedisDispatchDriver.class);
        RedisDelayedDispatchMover mover = new RedisDelayedDispatchMover(redis, codec, readyDriver, "dispatch:delayed");

        mover.moveReadyMessages(Instant.ofEpochMilli(1000));

        verify(readyDriver, never()).enqueue(Mockito.any());
        verify(zset, never()).remove(Mockito.anyString(), Mockito.any());
    }

    @Test
    void moveReadyMessagesFailsWhenDelayedPayloadCannotBeDecoded() {
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);
        Mockito.when(redis.opsForZSet()).thenReturn(zset);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisDispatchMessageCodec codec = new RedisDispatchMessageCodec(objectMapper);
        Mockito.when(zset.rangeByScore("dispatch:delayed", 0, 1000)).thenReturn(Set.of("not-json"));
        RedisDispatchDriver readyDriver = Mockito.mock(RedisDispatchDriver.class);
        RedisDelayedDispatchMover mover = new RedisDelayedDispatchMover(redis, codec, readyDriver, "dispatch:delayed");

        assertThrows(IllegalStateException.class, () -> mover.moveReadyMessages(Instant.ofEpochMilli(1000)));
        verify(readyDriver, never()).enqueue(Mockito.any());
    }
}
