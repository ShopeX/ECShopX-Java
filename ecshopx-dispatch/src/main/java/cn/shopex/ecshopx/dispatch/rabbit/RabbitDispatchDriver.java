/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.dispatch.rabbit;

import cn.shopex.ecshopx.dispatch.AsyncDispatchDriver;
import cn.shopex.ecshopx.dispatch.DispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitDispatchDriver implements AsyncDispatchDriver {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitDispatchMessageConverter converter;
    private final String exchange;
    private final String deadExchange;
    private final DispatchConsumerStateRecorder stateRecorder;

    public RabbitDispatchDriver(RabbitTemplate rabbitTemplate, RabbitDispatchMessageConverter converter, String exchange) {
        this(rabbitTemplate, converter, exchange, null);
    }

    public RabbitDispatchDriver(RabbitTemplate rabbitTemplate, RabbitDispatchMessageConverter converter, String exchange,
            DispatchConsumerStateRecorder stateRecorder) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
        this.exchange = exchange;
        this.deadExchange = exchange + ".dead";
        this.stateRecorder = stateRecorder;
    }

    @Override
    public void enqueue(DispatchMessage message) {
        String routingKey = message.queue() == null || message.queue().isBlank() ? "default" : message.queue();
        rabbitTemplate.convertAndSend(exchange, routingKey, converter.toBytes(message));
    }

    @Override
    public void enqueueDelayed(DispatchMessage message) {
        enqueue(message);
    }

    @Override
    public DispatchMessage consume(String payload) {
        return converter.fromBytes(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void ack(DispatchMessage message) {
        Objects.requireNonNull(message);
        if (stateRecorder != null) {
            stateRecorder.recordAck(message, 1);
        }
    }

    @Override
    public void retry(DispatchMessage message) {
        if (stateRecorder != null) {
            stateRecorder.recordRetry(message, 1);
        }
        enqueue(message);
    }

    @Override
    public void fail(DispatchMessage message) {
        if (stateRecorder != null) {
            stateRecorder.recordFail(message, 1);
        }
        rabbitTemplate.convertAndSend(deadExchange, message.queue() == null || message.queue().isBlank() ? "default" : message.queue(), converter.toBytes(message));
    }
}
