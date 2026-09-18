package cn.shopex.ecshopx.dispatch.rabbit;

import static org.mockito.Mockito.verify;

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
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitDispatchDriverTest {

    @Test
    void enqueueUsesDefaultQueueRoutingKeyWhenQueueMissing() {
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        RabbitDispatchDriver driver = new RabbitDispatchDriver(rabbitTemplate,
                new RabbitDispatchMessageConverter(new ObjectMapper().registerModule(new JavaTimeModule())), "dispatch.direct");

        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.JOB,
                DispatchMode.ASYNC,
                DispatchDriverType.RABBITMQ,
                "job:test",
                Map.of("id", 1),
                null,
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-rabbit",
                null);

        driver.enqueue(message);
        verify(rabbitTemplate).convertAndSend(Mockito.eq("dispatch.direct"), Mockito.eq("default"), Mockito.any(byte[].class));
    }

    @Test
    void ackDoesNotRepublishMessage() {
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        RabbitDispatchDriver driver = new RabbitDispatchDriver(rabbitTemplate,
                new RabbitDispatchMessageConverter(new ObjectMapper().registerModule(new JavaTimeModule())), "dispatch.direct");
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.RABBITMQ,
                "event:test",
                Map.of("id", 2),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-rabbit-2",
                "listener:sms");

        driver.ack(message);

        Mockito.verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void retryPublishesMessageBackToExchange() {
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        RabbitDispatchMessageConverter converter = new RabbitDispatchMessageConverter(new ObjectMapper().registerModule(new JavaTimeModule()));
        RabbitDispatchDriver driver = new RabbitDispatchDriver(rabbitTemplate, converter, "dispatch.direct");
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.RABBITMQ,
                "event:test",
                Map.of("id", 2),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-rabbit-2",
                "listener:sms");

        driver.retry(message);

        verify(rabbitTemplate).convertAndSend("dispatch.direct", "sms", converter.toBytes(message));
    }

    @Test
    void failPublishesMessageToDeadExchange() {
        RabbitTemplate rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        RabbitDispatchMessageConverter converter = new RabbitDispatchMessageConverter(new ObjectMapper().registerModule(new JavaTimeModule()));
        RabbitDispatchDriver driver = new RabbitDispatchDriver(rabbitTemplate, converter, "dispatch.direct");
        DispatchMessage message = new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                DispatchDriverType.RABBITMQ,
                "event:test",
                Map.of("id", 3),
                "sms",
                null,
                RetryPolicy.platformDefault(),
                Instant.parse("2026-05-02T00:00:00Z"),
                "trace-rabbit-3",
                "listener:sms");

        driver.fail(message);

        verify(rabbitTemplate).convertAndSend("dispatch.direct.dead", "sms", converter.toBytes(message));
    }
}
