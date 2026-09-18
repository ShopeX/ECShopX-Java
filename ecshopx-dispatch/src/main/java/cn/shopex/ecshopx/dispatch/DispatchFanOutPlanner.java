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

package cn.shopex.ecshopx.dispatch;

import java.time.Duration;
import java.util.List;

public class DispatchFanOutPlanner {

    private final DispatchRegistry registry;

    public DispatchFanOutPlanner(DispatchRegistry registry) {
        this.registry = registry;
    }

    public DispatchRegistry registry() {
        return registry;
    }

    public List<DispatchMessage> plan(DispatchMessage eventMessage) {
        return registry.eventListeners(eventMessage.messageName()).stream().map(listener -> {
            Duration delay = eventMessage.delay() != null ? eventMessage.delay() : listener.options().delay();
            return new DispatchMessage(
                DispatchMessageType.EVENT,
                DispatchMode.ASYNC,
                eventMessage.driverType(),
                eventMessage.messageName(),
                eventMessage.payload(),
                listener.options().queue() == null ? "default" : listener.options().queue(),
                delay,
                listener.options().retryPolicy(),
                eventMessage.occurredAt(),
                eventMessage.traceId(),
                listener.listenerName());
        }).toList();
    }
}
