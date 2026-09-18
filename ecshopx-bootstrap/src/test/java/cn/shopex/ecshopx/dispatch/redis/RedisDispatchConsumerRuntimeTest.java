package cn.shopex.ecshopx.dispatch.redis;

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

class RedisDispatchConsumerRuntimeTest {

    @Test
    void consumeDecodesMessage() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RedisDispatchDriver driver = new RedisDispatchDriver(null, new RedisDispatchMessageCodec(objectMapper), "queue:");
        RedisDispatchConsumer consumer = new RedisDispatchConsumer(driver);

        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.REDIS,
                "event:member_created",
                Map.of("memberId", 9L),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-1",
                "listener:sms");

        consumer.consume(objectMapper.writeValueAsString(message), 1);
        assertEquals(message.messageName(), objectMapper.readValue(objectMapper.writeValueAsString(message), DispatchMessage.class).messageName());
    }
}
