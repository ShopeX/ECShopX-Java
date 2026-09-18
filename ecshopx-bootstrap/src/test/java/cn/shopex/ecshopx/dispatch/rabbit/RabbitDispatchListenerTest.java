package cn.shopex.ecshopx.dispatch.rabbit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class RabbitDispatchListenerTest {

    @Test
    void onMessageDecodesAndDeliversMessage() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RabbitDispatchMessageConverter converter = new RabbitDispatchMessageConverter(objectMapper);
        DispatchConsumerRuntime runtime = mock(DispatchConsumerRuntime.class);
        RabbitDispatchListener listener = new RabbitDispatchListener(converter, runtime);

        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.RABBITMQ,
                "event:member_created",
                Map.of("memberId", 9L),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        listener.onMessage(converter.toBytes(message));

        verify(runtime).consume(ArgumentMatchers.argThat(actual ->
                actual.messageName().equals("event:member_created")
                        && actual.queue().equals("sms")
                        && actual.traceId().equals("trace-1")
                        && actual.listenerName().equals("listener:sms")
                        && actual.driverType() == DispatchDriverType.RABBITMQ),
                ArgumentMatchers.eq(1));
        assertNotNull(listener);
    }
}
