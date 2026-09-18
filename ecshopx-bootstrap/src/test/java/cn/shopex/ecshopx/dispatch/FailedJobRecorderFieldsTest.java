package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FailedJobRecorderFieldsTest {

    @Test
    void recordFailurePopulatesDriverDelayAndConsumerIdentity() {
        FailedJobMapper mapper = Mockito.mock(FailedJobMapper.class);
        FailedJobRecorder recorder = new FailedJobRecorder(mapper, new com.fasterxml.jackson.databind.ObjectMapper());
        DispatchMessage message = new DispatchMessage(
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
                "listener:sms");

        recorder.recordFailure(message, 3, "rabbit-consumer-1", new IllegalStateException("boom"));

        ArgumentCaptor<FailedJobEntity> captor = ArgumentCaptor.forClass(FailedJobEntity.class);
        verify(mapper).insert(captor.capture());
        assertEquals("event:member_created", captor.getValue().getMessageName());
        assertEquals("listener:sms", captor.getValue().getListenerName());
        assertEquals("sms", captor.getValue().getQueueName());
        assertEquals(3, captor.getValue().getRetryCount());
        assertEquals("java.lang.IllegalStateException", captor.getValue().getExceptionClass());
        assertEquals("boom", captor.getValue().getExceptionMessage());
        assertEquals("rabbit-consumer-1", captor.getValue().getConsumerId());
    }
}
