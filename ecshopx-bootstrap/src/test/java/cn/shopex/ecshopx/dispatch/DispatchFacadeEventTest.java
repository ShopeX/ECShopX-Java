package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchFacadeEventTest {

    @Test
    void publishEventDefaultsToSyncMode() {
        DispatchCore core = new DispatchCore(new InMemoryDispatchRegistry(), new SyncDispatchDriver(new InMemoryDispatchRegistry()));
        DispatchFacade facade = new DispatchFacade(core);

        facade.publishEvent("event:member_created", Map.of("memberId", 9L), DispatchOptions.eventDefaults());

        DispatchMessage message = facade.lastPublishedMessage();
        assertNotNull(message);
        assertEquals(DispatchMessageType.EVENT, message.messageType());
        assertEquals(DispatchMode.SYNC, message.dispatchMode());
        assertEquals("event:member_created", message.messageName());
    }

    @Test
    void publishEventCanSetAsyncEnvelopeMetadata() {
        DispatchCore core = DispatchCore.asyncReady(
                new InMemoryDispatchRegistry(),
                new SyncDispatchDriver(new InMemoryDispatchRegistry()),
                Map.of(DispatchDriverType.REDIS, message -> {}));
        DispatchFacade facade = new DispatchFacade(core);

        facade.publishEvent("event:member_created", Map.of("memberId", 9L), new DispatchOptions(DispatchMode.ASYNC, DispatchDriverType.REDIS, "sms", Duration.ofSeconds(5), RetryPolicy.platformDefault()));

        DispatchMessage message = facade.lastPublishedMessage();
        assertNotNull(message);
        assertEquals(DispatchMessageType.EVENT, message.messageType());
        assertEquals(DispatchMode.ASYNC, message.dispatchMode());
        assertEquals(DispatchDriverType.REDIS, message.driverType());
        assertEquals("sms", message.queue());
    }
}
