package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchStructuredLoggerFieldsTest {

    @Test
    void formatIncludesDriverQueueDelayRetryAndConsumerIdentity() {
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 9L),
                "sms",
                java.time.Duration.ofMinutes(2),
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        String formatted = new DispatchStructuredLogger().format("failed", message, 3, new IllegalStateException("boom"));

        assertTrue(formatted.contains("stage=failed"));
        assertTrue(formatted.contains("traceId=trace-1"));
        assertTrue(formatted.contains("driver=REDIS"));
        assertTrue(formatted.contains("queue=sms"));
        assertTrue(formatted.contains("listener=listener:sms"));
        assertTrue(formatted.contains("attempt=3"));
        assertTrue(formatted.contains("error=java.lang.IllegalStateException"));
        assertTrue(formatted.contains("consumerId=trace-1"));
    }
}
