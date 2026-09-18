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

public class DispatchConsumerRuntime {

    private final DispatchRegistry registry;
    private final DispatchRetryDecider retryDecider;
    private final FailedJobRecorder failedJobRecorder;
    private final DispatchStructuredLogger structuredLogger;
    private final DispatchConsumerStateRecorder stateRecorder;

    public DispatchConsumerRuntime(DispatchRegistry registry, DispatchRetryDecider retryDecider,
            FailedJobRecorder failedJobRecorder, DispatchStructuredLogger structuredLogger,
            DispatchConsumerStateRecorder stateRecorder) {
        this.registry = registry;
        this.retryDecider = retryDecider;
        this.failedJobRecorder = failedJobRecorder;
        this.structuredLogger = structuredLogger;
        this.stateRecorder = stateRecorder;
    }

    public void consume(DispatchMessage message, int currentAttempt) {
        try {
            if (message.messageType() == DispatchMessageType.JOB) {
                registry.jobHandler(message.messageName()).handle(message.payload());
            } else {
                registry.eventListeners(message.messageName()).stream()
                        .filter(it -> it.listenerName().equals(message.listenerName()))
                        .findFirst()
                        .orElseThrow()
                        .listener()
                        .onEvent(message.payload());
            }
            stateRecorder.recordAck(message, currentAttempt);
        } catch (RuntimeException e) {
            if (!retryDecider.shouldRetry(message, currentAttempt)) {
                structuredLogger.format("failed", message, currentAttempt, e);
                failedJobRecorder.recordFailure(message, currentAttempt, message.traceId(), e);
                stateRecorder.recordFail(message, currentAttempt);
                return;
            }
            stateRecorder.recordRetry(message, currentAttempt);
            throw e;
        }
    }
}
