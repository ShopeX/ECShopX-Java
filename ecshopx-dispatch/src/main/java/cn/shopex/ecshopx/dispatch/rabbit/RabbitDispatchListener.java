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

import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitDispatchListener {

    private final RabbitDispatchMessageConverter converter;
    private final DispatchConsumerRuntime runtime;

    public RabbitDispatchListener(RabbitDispatchMessageConverter converter, DispatchConsumerRuntime runtime) {
        this.converter = converter;
        this.runtime = runtime;
    }

    @RabbitListener(queues = "${dispatch.rabbit.queue:dispatch}")
    public void onMessage(byte[] payload) {
        runtime.consume(converter.fromBytes(payload), 1);
    }
}
