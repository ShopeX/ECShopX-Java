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

public record ListenerDispatchOptions(String queue, Duration delay, RetryPolicy retryPolicy) {

    public static ListenerDispatchOptions asyncDefaults() {
        return new ListenerDispatchOptions(null, null, RetryPolicy.platformDefault());
    }

    /**
     * Registration label for listeners driven on the synchronous {@link DispatchMode#SYNC} path ({@code
     * DispatchOptions.eventDefaults()}): same tuple as {@link #asyncDefaults()}, distinct name for call-site
     * documentation.
     */
    public static ListenerDispatchOptions syncDefaults() {
        return asyncDefaults();
    }

    public static ListenerDispatchOptions async(String queue, Duration delay) {
        return new ListenerDispatchOptions(queue, delay, RetryPolicy.platformDefault());
    }
}
