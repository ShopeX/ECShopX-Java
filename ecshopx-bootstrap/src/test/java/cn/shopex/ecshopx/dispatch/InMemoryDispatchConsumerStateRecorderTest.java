package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryDispatchConsumerStateRecorderTest {

    @Test
    void recordsAckRetryAndFailEvents() {
        InMemoryDispatchConsumerStateRecorder recorder = new InMemoryDispatchConsumerStateRecorder();
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:test",
                Map.of("id", 1),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        recorder.recordAck(message, 1);
        recorder.recordRetry(message, 1);
        recorder.recordFail(message, 1);

        assertEquals(
                java.util.List.of(
                        "ack:trace-1:listener:sms:1",
                        "retry:trace-1:listener:sms:1",
                        "fail:trace-1:listener:sms:1"),
                recorder.events());
    }
}
