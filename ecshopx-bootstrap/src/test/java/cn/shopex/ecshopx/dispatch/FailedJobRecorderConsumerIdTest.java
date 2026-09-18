package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FailedJobRecorderConsumerIdTest {

    @Test
    void recordFailureUsesExplicitConsumerId() {
        FailedJobMapper mapper = Mockito.mock(FailedJobMapper.class);
        FailedJobRecorder recorder = new FailedJobRecorder(mapper, new com.fasterxml.jackson.databind.ObjectMapper());
        DispatchEnvelope envelope = new DispatchEnvelope(
                new DispatchMessage(
                        DispatchMessageType.EVENT,
                        DispatchMode.ASYNC,
                        DispatchDriverType.RABBITMQ,
                        "event:member_created",
                        Map.of("memberId", 9L),
                        "sms",
                        java.time.Duration.ofMinutes(2),
                        RetryPolicy.platformDefault(),
                        Instant.parse("2026-05-02T00:00:00Z"),
                        "trace-1",
                        "listener:sms"),
                3,
                "rabbit-consumer-1");

        recorder.recordFailure(envelope.message(), envelope.attempt(), envelope.consumerId(), new IllegalStateException("boom"));

        ArgumentCaptor<FailedJobEntity> captor = ArgumentCaptor.forClass(FailedJobEntity.class);
        verify(mapper).insert(captor.capture());
        assertEquals("rabbit-consumer-1", captor.getValue().getConsumerId());
    }
}
