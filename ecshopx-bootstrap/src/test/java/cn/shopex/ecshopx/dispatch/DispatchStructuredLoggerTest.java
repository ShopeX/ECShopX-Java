package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchStructuredLoggerTest {

    @Test
    void formatsPublishLogWithTraceDriverQueueAndListener() {
        DispatchStructuredLogger logger = new DispatchStructuredLogger();
        String line = logger.format("enqueue", new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 1L),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-11",
                "listener:sms"), 1, null);

        assertTrue(line.contains("stage=enqueue"));
        assertTrue(line.contains("traceId=trace-11"));
        assertTrue(line.contains("driver=REDIS"));
        assertTrue(line.contains("queue=sms"));
        assertTrue(line.contains("listener=listener:sms"));
    }
}
