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

public class SyncDispatchDriver {

    private final DispatchRegistry registry;

    public SyncDispatchDriver(DispatchRegistry registry) {
        this.registry = registry;
    }

    public void execute(DispatchMessage message) {
        if (message.messageType() == DispatchMessageType.JOB) {
            registry.jobHandler(message.messageName()).handle(message.payload());
            return;
        }
        String targetListener = message.listenerName();
        for (RegisteredListener listener : registry.eventListeners(message.messageName())) {
            if (targetListener != null && !targetListener.equals(listener.listenerName())) {
                continue;
            }
            listener.listener().onEvent(message.payload());
        }
    }
}
