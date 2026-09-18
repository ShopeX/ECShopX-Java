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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderPickupSmsRedisVerifyService {

	private final StringRedisTemplate companysRedisTemplate;

	public OrderPickupSmsRedisVerifyService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void verifyAndConsumePickupCode(long orderId, String phoneRaw, String pickupcodeRaw) {
		if (orderId <= 0L) {
			throw new ResourceException("提货码验证错误");
		}
		if (phoneRaw == null || phoneRaw.trim().isEmpty()) {
			throw new ResourceException("提货码验证错误");
		}
		String phone = phoneRaw.trim();
		if (pickupcodeRaw == null) {
			throw new ResourceException("提货码验证错误");
		}
		String submitted = pickupcodeRaw.trim();
		if (submitted.isEmpty()) {
			throw new ResourceException("提货码验证错误");
		}
		String key = "admin-pickupcode:" + orderId + "|" + phone;
		String stored = companysRedisTemplate.opsForValue().get(key);
		if (stored == null) {
			throw new ResourceException("提货码验证错误");
		}
		String vcode = stored.trim();
		if (!(submitted.equals(vcode) || looseNumericEquals(submitted, vcode))) {
			throw new ResourceException("提货码验证错误");
		}
		companysRedisTemplate.delete(key);
	}

	private static boolean looseNumericEquals(String a, String b) {
		try {
			return Long.parseLong(a) == Long.parseLong(b);
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
