package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchFacadeAsyncEventTest {

    @Test
    void asyncEventFansOutToRegisteredListeners() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerEventListener("event:member_created", "listener:sms",
                new ListenerDispatchOptions("sms", Duration.ofMinutes(2), RetryPolicy.platformDefault()), payload -> {});
        registry.registerEventListener("event:member_created", "listener:default",
                ListenerDispatchOptions.asyncDefaults(), payload -> {});

        DispatchFacade facade = new DispatchFacade(
                DispatchCore.asyncReady(
                        registry,
                        new SyncDispatchDriver(registry),
                        Map.of(DispatchDriverType.REDIS, message -> {})),
                new DispatchFanOutPlanner(registry));

        facade.publishEvent("event:member_created", Map.of("memberId", 9L),
                new DispatchOptions(DispatchMode.ASYNC, DispatchDriverType.REDIS, null, null, RetryPolicy.platformDefault()));

        List<DispatchMessage> published = facade.publishedMessages();
        assertEquals(2, published.size());
        assertEquals("listener:sms", published.get(0).listenerName());
        assertEquals("sms", published.get(0).queue());
        assertEquals(Duration.ofMinutes(2), published.get(0).delay());
        assertEquals("listener:default", published.get(1).listenerName());
        assertEquals("default", published.get(1).queue());
    }
}
