package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchFanOutPlannerTest {

    @Test
    void asyncEventUsesListenerLevelQueueAndDelay() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerEventListener("event:member_created", "listener:sms",
                new ListenerDispatchOptions("sms", Duration.ofMinutes(2), RetryPolicy.platformDefault()), payload -> {});
        registry.registerEventListener("event:member_created", "listener:default",
                ListenerDispatchOptions.asyncDefaults(), payload -> {});

        DispatchFanOutPlanner planner = new DispatchFanOutPlanner(registry);
        List<DispatchMessage> planned = planner.plan(new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 9L),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-event",
                null));

        assertEquals("sms", planned.get(0).queue());
        assertEquals(Duration.ofMinutes(2), planned.get(0).delay());
        assertEquals("default", planned.get(1).queue());
        assertEquals(null, planned.get(1).delay());
    }

    @Test
    void asyncEventPrefersParentMessageDelayWhenPresent() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerEventListener(
                "event:orders:wx_order_shipping",
                "listener:wx-order-shipping",
                ListenerDispatchOptions.asyncDefaults(),
                payload -> {});

        DispatchFanOutPlanner planner = new DispatchFanOutPlanner(registry);
        List<DispatchMessage> planned = planner.plan(new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:orders:wx_order_shipping",
                Map.of("orderId", 1L),
                null,
                Duration.ofSeconds(3),
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-event",
                null));

        assertEquals(Duration.ofSeconds(3), planned.get(0).delay());
    }
}
