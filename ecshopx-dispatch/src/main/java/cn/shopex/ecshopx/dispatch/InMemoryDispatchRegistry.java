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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryDispatchRegistry implements DispatchRegistry {

    private final Map<String, DispatchHandler> jobs = new LinkedHashMap<>();
    private final Map<String, List<RegisteredListener>> events = new LinkedHashMap<>();

    @Override
    public void registerJob(String messageName, DispatchHandler handler) {
        if (jobs.containsKey(messageName)) {
            throw new IllegalStateException("duplicate job handler: " + messageName);
        }
        jobs.put(messageName, handler);
    }

    @Override
    public void registerEventListener(String messageName, String listenerName, ListenerDispatchOptions options,
            DispatchListener listener) {
        List<RegisteredListener> list = events.computeIfAbsent(messageName, key -> new ArrayList<>());
        for (RegisteredListener existing : list) {
            if (existing.listenerName().equals(listenerName)) {
                throw new IllegalStateException("duplicate event listener: " + messageName + " / " + listenerName);
            }
        }
        list.add(new RegisteredListener(listenerName, options, listener));
    }

    @Override
    public DispatchHandler jobHandler(String messageName) {
        return jobs.get(messageName);
    }

    @Override
    public List<RegisteredListener> eventListeners(String messageName) {
        return events.getOrDefault(messageName, List.of());
    }
}
