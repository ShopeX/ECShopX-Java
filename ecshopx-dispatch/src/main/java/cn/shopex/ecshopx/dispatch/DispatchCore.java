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

import java.util.Map;

public class DispatchCore {

    private final DispatchRegistry registry;
    private final SyncDispatchDriver syncDispatchDriver;
    private final Map<DispatchDriverType, AsyncDispatchDriver> asyncDrivers;

    public DispatchCore(DispatchRegistry registry, SyncDispatchDriver syncDispatchDriver) {
        this(registry, syncDispatchDriver, Map.of());
    }

    private DispatchCore(DispatchRegistry registry, SyncDispatchDriver syncDispatchDriver,
            Map<DispatchDriverType, AsyncDispatchDriver> asyncDrivers) {
        this.registry = registry;
        this.syncDispatchDriver = syncDispatchDriver;
        this.asyncDrivers = asyncDrivers;
    }

    public static DispatchCore asyncReady(DispatchRegistry registry, SyncDispatchDriver syncDispatchDriver,
            Map<DispatchDriverType, AsyncDispatchDriver> asyncDrivers) {
        return new DispatchCore(registry, syncDispatchDriver, asyncDrivers);
    }

    public void dispatch(DispatchMessage message) {
        if (message.dispatchMode() == DispatchMode.SYNC) {
            syncDispatchDriver.execute(message);
            return;
        }
        AsyncDispatchDriver driver = asyncDrivers.get(message.driverType());
        if (driver == null) {
            throw new IllegalStateException("missing async driver: " + message.driverType());
        }
        driver.enqueue(message);
    }
}
