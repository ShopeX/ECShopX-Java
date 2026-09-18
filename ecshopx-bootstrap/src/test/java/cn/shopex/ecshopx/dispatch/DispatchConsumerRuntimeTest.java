package cn.shopex.ecshopx.dispatch;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DispatchConsumerRuntimeTest {

    @Test
    void failedListenerPastMaxAttemptsWritesFailedJobs() {
        InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
        registry.registerEventListener("event:member_created", "listener:sms",
                ListenerDispatchOptions.asyncDefaults(), payload -> {
                    throw new IllegalStateException("boom");
                });
        FailedJobRecorder recorder = Mockito.mock(FailedJobRecorder.class);
        DispatchStructuredLogger logger = Mockito.mock(DispatchStructuredLogger.class);
        InMemoryDispatchConsumerStateRecorder stateRecorder = new InMemoryDispatchConsumerStateRecorder();
        DispatchConsumerRuntime runtime = new DispatchConsumerRuntime(registry, new DispatchRetryDecider(), recorder, logger, stateRecorder);
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 9L),
                "default",
                null,
                new RetryPolicy(1, java.time.Duration.ofSeconds(3)),
                Instant.now(),
                "trace-rt",
                "listener:sms");

        runtime.consume(message, 1);
        verify(logger).format(Mockito.eq("failed"), Mockito.eq(message), Mockito.eq(1), Mockito.any(IllegalStateException.class));
        verify(recorder).recordFailure(Mockito.eq(message), Mockito.eq(1), Mockito.eq("trace-rt"), Mockito.any(IllegalStateException.class));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("fail:trace-rt:listener:sms:1"), stateRecorder.events());
    }
}
