package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DispatchCoreAsyncRoutingTest {

    @Test
    void asyncJobRoutesToSelectedDriver() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        AtomicInteger redisCalls = new AtomicInteger();
        AtomicInteger rabbitCalls = new AtomicInteger();
        AsyncDispatchDriver redis = message -> redisCalls.incrementAndGet();
        AsyncDispatchDriver rabbit = message -> rabbitCalls.incrementAndGet();
        DispatchCore core = DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(
                DispatchDriverType.REDIS, redis,
                DispatchDriverType.RABBITMQ, rabbit));

        core.dispatch(new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "job:redis",
                Map.of(),
                "slow",
                null,
                RetryPolicy.platformDefault(),
                Instant.now(),
                "trace-redis",
                null));

        assertEquals(1, redisCalls.get());
        assertEquals(0, rabbitCalls.get());
    }
}
