package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryDispatchRegistryTest {

    @Test
    void jobRegistrationRejectsDuplicates() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerJob("job:merchant_sms", payload -> {});
        assertThrows(IllegalStateException.class, () -> registry.registerJob("job:merchant_sms", payload -> {}));
    }

    @Test
    void eventRegistrationSupportsMultipleListeners() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerEventListener("event:member_created", "listener:a", ListenerDispatchOptions.asyncDefaults(), payload -> {});
        registry.registerEventListener("event:member_created", "listener:b", ListenerDispatchOptions.asyncDefaults(), payload -> {});
        assertEquals(List.of("listener:a", "listener:b"),
                registry.eventListeners("event:member_created").stream().map(RegisteredListener::listenerName).toList());
    }
}
