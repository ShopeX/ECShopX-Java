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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DispatchFacade {

    private final DispatchCore core;
    private final DispatchFanOutPlanner fanOutPlanner;
    private final List<DispatchMessage> publishedMessages = new ArrayList<>();

    public DispatchFacade(DispatchCore core) {
        this(core, null);
    }

    public DispatchFacade(DispatchCore core, DispatchFanOutPlanner fanOutPlanner) {
        this.core = core;
        this.fanOutPlanner = fanOutPlanner;
    }

    public void dispatchJob(String messageName, Map<String, Object> payload, DispatchOptions options) {
        DispatchMessage message = buildMessage(DispatchMessageType.JOB, messageName, payload, options, null);
        publishedMessages.add(message);
        core.dispatch(message);
    }

    public void publishEvent(String messageName, Map<String, Object> payload, DispatchOptions options) {
        if (options.mode() == DispatchMode.ASYNC && fanOutPlanner != null) {
            DispatchMessage message = buildMessage(DispatchMessageType.EVENT, messageName, payload, options, null);
            for (DispatchMessage planned : fanOutPlanner.plan(message)) {
                publishedMessages.add(planned);
                core.dispatch(planned);
            }
            return;
        }
        if (options.mode() == DispatchMode.SYNC && fanOutPlanner != null) {
            List<RegisteredListener> listeners = fanOutPlanner.registry().eventListeners(messageName);
            if (listeners.size() == 1) {
                RegisteredListener only = listeners.get(0);
                DispatchMessage template =
                        buildMessage(DispatchMessageType.EVENT, messageName, payload, options, null);
                DispatchMessage parent = withListenerName(template, only.listenerName());
                publishedMessages.add(parent);
                DispatchMessage child = withListenerName(template, only.listenerName());
                publishedMessages.add(child);
                core.dispatch(child);
                return;
            }
        }
        DispatchMessage message = buildMessage(DispatchMessageType.EVENT, messageName, payload, options, null);
        publishedMessages.add(message);
        core.dispatch(message);
    }

    public DispatchMessage lastPublishedMessage() {
        return publishedMessages.isEmpty() ? null : publishedMessages.get(publishedMessages.size() - 1);
    }

    public List<DispatchMessage> publishedMessages() {
        return List.copyOf(publishedMessages);
    }

    private DispatchMessage buildMessage(DispatchMessageType messageType, String messageName, Map<String, Object> payload,
            DispatchOptions options, String listenerName) {
        return new DispatchMessage(
                messageType,
                options.mode(),
                options.mode() == DispatchMode.SYNC ? DispatchDriverType.SYNC : options.driverOverride(),
                messageName,
                payload,
                options.queue(),
                options.delay(),
                options.retryPolicy(),
                Instant.now(),
                UUID.randomUUID().toString(),
                listenerName);
    }

    private static DispatchMessage withListenerName(DispatchMessage template, String listenerName) {
        return new DispatchMessage(
                template.messageType(),
                template.dispatchMode(),
                template.driverType(),
                template.messageName(),
                template.payload(),
                template.queue(),
                template.delay(),
                template.retryPolicy(),
                template.occurredAt(),
                template.traceId(),
                listenerName);
    }
}
