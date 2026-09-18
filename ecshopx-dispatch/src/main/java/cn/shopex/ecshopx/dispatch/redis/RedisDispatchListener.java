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

import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisDispatchListener {

    private final StringRedisTemplate redis;
    private final RedisDispatchMessageCodec codec;
    private final DispatchConsumerRuntime runtime;
    private final String readyQueuePrefix;
    private final int slowQueueBatchSize;

    @Autowired
    public RedisDispatchListener(
            @Qualifier("dispatchBusStringRedisTemplate") StringRedisTemplate redis,
            RedisDispatchMessageCodec codec,
            DispatchConsumerRuntime runtime,
            @Value("${dispatch.redis.slow.batch-size:20}") int slowQueueBatchSize) {
        this(redis, codec, runtime, "dispatch:", slowQueueBatchSize);
    }

    public RedisDispatchListener(StringRedisTemplate redis, RedisDispatchMessageCodec codec,
            DispatchConsumerRuntime runtime, String readyQueuePrefix) {
        this(redis, codec, runtime, readyQueuePrefix, 20);
    }

    public RedisDispatchListener(StringRedisTemplate redis, RedisDispatchMessageCodec codec,
            DispatchConsumerRuntime runtime, String readyQueuePrefix, int slowQueueBatchSize) {
        this.redis = redis;
        this.codec = codec;
        this.runtime = runtime;
        this.readyQueuePrefix = readyQueuePrefix;
        this.slowQueueBatchSize = slowQueueBatchSize;
    }

    @Scheduled(fixedDelayString = "${dispatch.redis.poll-delay-ms:1000}")
    public void poll() {
        String payload = redis.opsForList().leftPop(readyQueuePrefix + "default");
        if (payload != null) {
            runtime.consume(codec.decode(payload), 1);
        }
        for (int i = 0; i < slowQueueBatchSize; i++) {
            String slowPayload = redis.opsForList().leftPop(readyQueuePrefix + "slow");
            if (slowPayload == null) {
                break;
            }
            runtime.consume(codec.decode(slowPayload), 1);
        }
        for (int i = 0; i < slowQueueBatchSize; i++) {
            String smsPayload = redis.opsForList().leftPop(readyQueuePrefix + "sms");
            if (smsPayload == null) {
                break;
            }
            runtime.consume(codec.decode(smsPayload), 1);
        }
    }
}
