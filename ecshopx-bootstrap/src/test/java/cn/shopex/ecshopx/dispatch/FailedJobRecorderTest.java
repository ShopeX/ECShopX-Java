package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FailedJobRecorderTest {

    @Test
    void listenerFailurePersistsIntoFailedJobs() {
        FailedJobMapper mapper = Mockito.mock(FailedJobMapper.class);
        FailedJobRecorder recorder = new FailedJobRecorder(mapper, new ObjectMapper());
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 1L),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        recorder.recordFailure(message, 3, "consumer-1", new IllegalStateException("boom"));

        ArgumentCaptor<FailedJobEntity> captor = ArgumentCaptor.forClass(FailedJobEntity.class);
        Mockito.verify(mapper).insert(captor.capture());
        assertEquals("event:member_created", captor.getValue().getMessageName());
        assertEquals("listener:sms", captor.getValue().getListenerName());
        assertEquals("sms", captor.getValue().getQueueName());
    }
}
