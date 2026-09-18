package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DispatchFacadeDefaultsTest {

    @Test
    void jobDefaultsToAsyncWithDefaultDriverAndNoDelay() {
        DispatchOptions options = DispatchOptions.jobDefaults();
        assertEquals(DispatchMode.ASYNC, options.mode());
        assertNull(options.driverOverride());
        assertNull(options.queue());
        assertNull(options.delay());
        assertEquals(RetryPolicy.platformDefault(), options.retryPolicy());
    }

    @Test
    void eventDefaultsToSyncWithoutQueueOrDelay() {
        DispatchOptions options = DispatchOptions.eventDefaults();
        assertEquals(DispatchMode.SYNC, options.mode());
        assertNull(options.driverOverride());
        assertNull(options.queue());
        assertNull(options.delay());
    }

    @Test
    void listenerAsyncDefaultsUsePlatformFallbacks() {
        ListenerDispatchOptions options = ListenerDispatchOptions.asyncDefaults();
        assertNull(options.queue());
        assertNull(options.delay());
        assertEquals(RetryPolicy.platformDefault(), options.retryPolicy());
    }

    @Test
    void listenerDelayCanBeDeclaredIndependently() {
        ListenerDispatchOptions options = ListenerDispatchOptions.async("sms", Duration.ofMinutes(2));
        assertEquals("sms", options.queue());
        assertEquals(Duration.ofMinutes(2), options.delay());
    }
}
