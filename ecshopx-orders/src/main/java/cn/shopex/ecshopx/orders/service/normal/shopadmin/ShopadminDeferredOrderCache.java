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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopadminDeferredOrderCache {

	private static final String KEY_PREFIX = "shopadmin:deferred:";
	private static final String LOCK_PREFIX = "shopadmin:deferred:lock:";
	private static final Duration LOCK_TTL = Duration.ofSeconds(120);

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public ShopadminDeferredOrderCache(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void saveWaitPay(
			NormalOrderCreateState state,
			long orderId,
			long companyId,
			String payType,
			int totalFee,
			int ttlSeconds) {
		long now = System.currentTimeMillis() / 1000L;
		ShopadminDeferredOrderSnapshot snap = snapshotFromState(state, orderId, companyId, payType, totalFee, now);
		snap.setStatus(ShopadminDeferredOrderStatus.WAIT_PAY);
		snap.setExpireAt(now + ttlSeconds);
		write(snap, ttlSeconds);
	}

	public ShopadminDeferredOrderSnapshot get(long companyId, long orderId) {
		String raw;
		try {
			raw = sharedStringRedisTemplate.opsForValue().get(key(companyId, orderId));
		} catch (DataAccessException e) {
			throw new ResourceException("订单缓存不可用");
		}
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return objectMapper.readValue(raw, ShopadminDeferredOrderSnapshot.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public void updateSnapshot(ShopadminDeferredOrderSnapshot snap, int ttlSeconds) {
		write(snap, ttlSeconds);
	}

	public void delete(long companyId, long orderId) {
		try {
			sharedStringRedisTemplate.delete(key(companyId, orderId));
		} catch (DataAccessException e) {
			// best-effort
		}
	}

	public <T> T withLock(long orderId, java.util.function.Supplier<T> action) {
		String lockKey = LOCK_PREFIX + orderId;
		String token = UUID.randomUUID().toString();
		Boolean locked;
		try {
			locked = sharedStringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);
		} catch (DataAccessException e) {
			throw new ResourceException("订单处理繁忙，请稍后查询");
		}
		if (!Boolean.TRUE.equals(locked)) {
			throw new ResourceException("订单处理中，请稍后查询");
		}
		try {
			return action.get();
		} finally {
			try {
				String current = sharedStringRedisTemplate.opsForValue().get(lockKey);
				if (token.equals(current)) {
					sharedStringRedisTemplate.delete(lockKey);
				}
			} catch (RuntimeException ignored) {
				// best-effort unlock
			}
		}
	}

	private void write(ShopadminDeferredOrderSnapshot snap, int ttlSeconds) {
		String json;
		try {
			json = objectMapper.writeValueAsString(snap);
		} catch (JsonProcessingException e) {
			throw new ResourceException("订单缓存写入失败");
		}
		try {
			sharedStringRedisTemplate
					.opsForValue()
					.set(key(snap.getCompanyId(), snap.getOrderId()), json, Duration.ofSeconds(ttlSeconds));
		} catch (DataAccessException e) {
			throw new ResourceException("订单缓存不可用");
		}
	}

	private static ShopadminDeferredOrderSnapshot snapshotFromState(
			NormalOrderCreateState state,
			long orderId,
			long companyId,
			String payType,
			int totalFee,
			long createdAt) {
		ShopadminDeferredOrderSnapshot snap = new ShopadminDeferredOrderSnapshot();
		snap.setOrderId(orderId);
		snap.setCompanyId(companyId);
		snap.setPayType(payType);
		snap.setTotalFee(totalFee);
		snap.setCreatedAt(createdAt);
		snap.setParams(new LinkedHashMap<>(state.getParams()));
		snap.setOrderData(new LinkedHashMap<>(state.getOrderData()));
		return snap;
	}

	private static String key(long companyId, long orderId) {
		return KEY_PREFIX + companyId + ":" + orderId;
	}
}
