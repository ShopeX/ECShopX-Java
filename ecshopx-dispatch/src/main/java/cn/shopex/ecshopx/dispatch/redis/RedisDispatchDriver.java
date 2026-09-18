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

import cn.shopex.ecshopx.dispatch.AsyncDispatchDriver;
import cn.shopex.ecshopx.dispatch.DispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisDispatchDriver implements AsyncDispatchDriver {

	private final StringRedisTemplate redis;
	private final RedisDispatchMessageCodec codec;
	private final String readyQueuePrefix;
	private final String deadQueuePrefix;
	private final DispatchConsumerStateRecorder stateRecorder;
	private final String delayedZSetKey;

	public RedisDispatchDriver(StringRedisTemplate redis, RedisDispatchMessageCodec codec, String readyQueuePrefix) {
		this(redis, codec, readyQueuePrefix, null, null);
	}

	public RedisDispatchDriver(
			StringRedisTemplate redis,
			RedisDispatchMessageCodec codec,
			String readyQueuePrefix,
			DispatchConsumerStateRecorder stateRecorder) {
		this(redis, codec, readyQueuePrefix, stateRecorder, null);
	}

	public RedisDispatchDriver(
			StringRedisTemplate redis,
			RedisDispatchMessageCodec codec,
			String readyQueuePrefix,
			DispatchConsumerStateRecorder stateRecorder,
			String delayedZSetKey) {
		this.redis = redis;
		this.codec = codec;
		this.readyQueuePrefix = readyQueuePrefix;
		this.deadQueuePrefix = readyQueuePrefix + "dead:";
		this.stateRecorder = stateRecorder;
		this.delayedZSetKey = delayedZSetKey;
	}

	@Override
	public void enqueue(DispatchMessage message) {
		String queue = message.queue() == null || message.queue().isBlank() ? "default" : message.queue();
		Duration delay = message.delay();
		if (delayedZSetKey != null
				&& !delayedZSetKey.isBlank()
				&& delay != null
				&& !delay.isZero()) {
			String encoded = codec.encode(message);
			double score = (double) (Instant.now().toEpochMilli() + delay.toMillis());
			redis.opsForZSet().add(delayedZSetKey, encoded, score);
			return;
		}
		redis.opsForList().rightPush(readyQueuePrefix + queue, codec.encode(message));
	}

	@Override
	public void enqueueDelayed(DispatchMessage message) {
		enqueue(message);
	}

	@Override
	public DispatchMessage consume(String payload) {
		return codec.decode(payload);
	}

	@Override
	public void ack(DispatchMessage message) {
		if (stateRecorder != null) {
			stateRecorder.recordAck(message, 1);
		}
	}

	@Override
	public void retry(DispatchMessage message) {
		if (stateRecorder != null) {
			stateRecorder.recordRetry(message, 1);
		}
		enqueue(message);
	}

	@Override
	public void fail(DispatchMessage message) {
		if (stateRecorder != null) {
			stateRecorder.recordFail(message, 1);
		}
		redis.opsForList()
				.rightPush(
						deadQueuePrefix
								+ (message.queue() == null || message.queue().isBlank() ? "default" : message.queue()),
						codec.encode(message));
	}
}
