package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchCoreSyncTest {

    @Test
    void syncJobCallsSingleHandler() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        List<String> trace = new ArrayList<>();
        registry.registerJob("job:one", payload -> trace.add("job:" + payload.get("id")));
        DispatchCore core = new DispatchCore(registry, new SyncDispatchDriver(registry));

        core.dispatch(new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.SYNC,
                DispatchDriverType.SYNC,
                "job:one",
                Map.of("id", 7L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.now(),
                "trace-1",
                null));

        assertEquals(List.of("job:7"), trace);
    }

    @Test
    void syncEventCallsAllListenersInRegistrationOrder() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        List<String> trace = new ArrayList<>();
        registry.registerEventListener("event:one", "listener:a", ListenerDispatchOptions.asyncDefaults(), payload -> trace.add("A"));
        registry.registerEventListener("event:one", "listener:b", ListenerDispatchOptions.asyncDefaults(), payload -> trace.add("B"));
        DispatchCore core = new DispatchCore(registry, new SyncDispatchDriver(registry));

        core.dispatch(new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.SYNC,
                DispatchDriverType.SYNC,
                "event:one",
                Map.of("id", 7L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.now(),
                "trace-2",
                null));

        assertEquals(List.of("A", "B"), trace);
    }
}
