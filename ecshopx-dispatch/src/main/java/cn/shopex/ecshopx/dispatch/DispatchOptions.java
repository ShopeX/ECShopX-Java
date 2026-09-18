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

public record DispatchOptions(
        DispatchMode mode,
        DispatchDriverType driverOverride,
        String queue,
        Duration delay,
        RetryPolicy retryPolicy) {

    public static DispatchOptions jobDefaults() {
        return new DispatchOptions(DispatchMode.ASYNC, null, null, null, RetryPolicy.platformDefault());
    }

    public static DispatchOptions eventDefaults() {
        return new DispatchOptions(DispatchMode.SYNC, null, null, null, RetryPolicy.platformDefault());
    }

    /**
     * Order process log fan-out: async parent message with Redis driver; listener queue/delay come from
     * {@link ListenerDispatchOptions} registration. Used after DB commit when publishing via the OPL port.
     */
    public static DispatchOptions oplQueuedAfterCommit() {
        return new DispatchOptions(
                DispatchMode.ASYNC, DispatchDriverType.REDIS, null, null, RetryPolicy.platformDefault());
    }
}
