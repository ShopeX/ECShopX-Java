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

public class DispatchStructuredLogger {

    public String format(String stage, DispatchMessage message, int attempt, Throwable throwable) {
        return "stage=" + stage
                + " traceId=" + message.traceId()
                + " driver=" + message.driverType()
                + " queue=" + message.queue()
                + " listener=" + message.listenerName()
                + " attempt=" + attempt
                + (throwable == null ? "" : " error=" + throwable.getClass().getName())
                + " consumerId=" + message.traceId();
    }
}
