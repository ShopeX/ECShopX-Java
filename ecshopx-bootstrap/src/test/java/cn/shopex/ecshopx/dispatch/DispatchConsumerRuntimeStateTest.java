package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DispatchConsumerRuntimeStateTest {

    @Test
    void recordsAckOnSuccessfulConsumption() {
        DispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerJob("job:test", payload -> { });
        DispatchRetryDecider retryDecider = new DispatchRetryDecider();
        FailedJobRecorder failedJobRecorder = mock(FailedJobRecorder.class);
        DispatchStructuredLogger structuredLogger = mock(DispatchStructuredLogger.class);
        InMemoryDispatchConsumerStateRecorder stateRecorder = new InMemoryDispatchConsumerStateRecorder();
        DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(registry, retryDecider, failedJobRecorder, structuredLogger, stateRecorder);
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "job:test",
                Map.of("id", 1),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                null);

        runtime.consume(message, 1);

        assertEquals(List.of("ack:trace-1:null:1"), stateRecorder.events());
        verify(failedJobRecorder, org.mockito.Mockito.never()).recordFailure(org.mockito.Mockito.any(), org.mockito.Mockito.anyInt(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any());
    }

    @Test
    void recordsRetryWhenConsumerThrowsAndRetryRemains() {
        DispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerJob("job:test", payload -> { throw new RuntimeException("boom"); });
        DispatchRetryDecider retryDecider = mock(DispatchRetryDecider.class);
        when(retryDecider.shouldRetry(org.mockito.Mockito.any(), org.mockito.Mockito.eq(1))).thenReturn(true);
        FailedJobRecorder failedJobRecorder = mock(FailedJobRecorder.class);
        DispatchStructuredLogger structuredLogger = mock(DispatchStructuredLogger.class);
        InMemoryDispatchConsumerStateRecorder stateRecorder = new InMemoryDispatchConsumerStateRecorder();
        DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(registry, retryDecider, failedJobRecorder, structuredLogger, stateRecorder);
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "job:test",
                Map.of("id", 1),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-2",
                null);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> runtime.consume(message, 1));
        assertEquals(List.of("retry:trace-2:null:1"), stateRecorder.events());
    }
}
