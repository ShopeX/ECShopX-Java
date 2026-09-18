package cn.shopex.ecshopx.dispatch.rabbit;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class RabbitDispatchConsumerRuntimeTest {

    @Test
    void consumeHandsOffDecodedMessageToRuntime() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RabbitDispatchDriver driver = new RabbitDispatchDriver(null, new RabbitDispatchMessageConverter(objectMapper), "dispatch.exchange");
        RabbitDispatchConsumer consumer = new RabbitDispatchConsumer(driver);

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

        consumer.consume(objectMapper.writeValueAsBytes(message), 1);
        assertEquals(message.messageName(), objectMapper.readValue(objectMapper.writeValueAsBytes(message), DispatchMessage.class).messageName());
    }
}
