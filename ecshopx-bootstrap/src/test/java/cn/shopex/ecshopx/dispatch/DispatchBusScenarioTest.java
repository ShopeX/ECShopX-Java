package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import cn.shopex.ecshopx.dispatch.redis.RedisDispatchDriver;
import cn.shopex.ecshopx.dispatch.redis.RedisDispatchMessageCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class DispatchBusScenarioTest {

    @Test
    void syncJobAndEventListenerRunInline() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        List<String> trace = new ArrayList<>();
        registry.registerJob("job:sync", payload -> trace.add("job:" + payload.get("id")));
        registry.registerEventListener("event:sync", "listener:first", ListenerDispatchOptions.asyncDefaults(), payload -> trace.add("event:first"));
        registry.registerEventListener("event:sync", "listener:second", ListenerDispatchOptions.asyncDefaults(), payload -> trace.add("event:second"));
        DispatchCore core = new DispatchCore(registry, new SyncDispatchDriver(registry));

        core.dispatch(new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.SYNC,
                DispatchDriverType.SYNC,
                "job:sync",
                Map.of("id", 1L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.now(),
                "trace-sync-job",
                null));

        core.dispatch(new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.SYNC,
                DispatchDriverType.SYNC,
                "event:sync",
                Map.of("id", 2L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.now(),
                "trace-sync-event",
                null));

        assertEquals(List.of("job:1", "event:first", "event:second"), trace);
    }

    @Test
    void redisAsyncEventFansOutToListenerTasks() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerEventListener("event:async", "listener:sms",
                new ListenerDispatchOptions("sms", null, RetryPolicy.platformDefault()), payload -> {});
        registry.registerEventListener("event:async", "listener:default",
                ListenerDispatchOptions.asyncDefaults(), payload -> {});

        DispatchFanOutPlanner planner = new DispatchFanOutPlanner(registry);
        List<DispatchMessage> tasks = planner.plan(new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:async",
                Map.of("id", 3L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-async-event",
                null));

        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> ops = Mockito.mock(ListOperations.class);
        Mockito.when(redis.opsForList()).thenReturn(ops);
        RedisDispatchDriver driver = new RedisDispatchDriver(
                redis,
                new RedisDispatchMessageCodec(new ObjectMapper().registerModule(new JavaTimeModule())),
                "dispatch:ready:");

        for (DispatchMessage task : tasks) {
            driver.enqueue(task);
        }

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(ops, Mockito.times(2)).rightPush(queueCaptor.capture(), payloadCaptor.capture());
        assertEquals(List.of("dispatch:ready:sms", "dispatch:ready:default"), queueCaptor.getAllValues());
        assertEquals("listener:sms", tasks.get(0).listenerName());
        assertEquals("listener:default", tasks.get(1).listenerName());
        assertNotNull(payloadCaptor.getAllValues().get(0));
        assertNotNull(payloadCaptor.getAllValues().get(1));
    }
}
