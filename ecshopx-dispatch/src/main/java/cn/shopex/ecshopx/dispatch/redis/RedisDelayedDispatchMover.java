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

package cn.shopex.ecshopx.dispatch.redis;

import cn.shopex.ecshopx.dispatch.DispatchMessage;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

public class RedisDelayedDispatchMover {

    private final StringRedisTemplate redis;
    private final RedisDispatchMessageCodec codec;
    private final RedisDispatchDriver readyDriver;
    private final String delayedQueueKey;

    public RedisDelayedDispatchMover(
            StringRedisTemplate redis,
            RedisDispatchMessageCodec codec,
            RedisDispatchDriver readyDriver,
            String delayedQueueKey) {
        this.redis = redis;
        this.codec = codec;
        this.readyDriver = readyDriver;
        this.delayedQueueKey = delayedQueueKey;
    }

    public void moveReadyMessages(Instant now) {
        ZSetOperations<String, String> zset = redis.opsForZSet();
        Set<String> payloads = zset.rangeByScore(delayedQueueKey, 0, now.toEpochMilli());
        if (payloads == null || payloads.isEmpty()) {
            return;
        }
        for (String payload : payloads) {
            DispatchMessage message = codec.decode(payload);
            DispatchMessage cleared =
                    new DispatchMessage(
                            message.messageType(),
                            message.dispatchMode(),
                            message.driverType(),
                            message.messageName(),
                            message.payload(),
                            message.queue(),
                            null,
                            message.retryPolicy(),
                            message.occurredAt(),
                            message.traceId(),
                            message.listenerName());
            readyDriver.enqueue(cleared);
            zset.remove(delayedQueueKey, payload);
        }
    }
}
