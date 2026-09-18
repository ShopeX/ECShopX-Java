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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseInviteRedisService {

	private final StringRedisTemplate stringRedisTemplate;

	public EmployeePurchaseInviteRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	private static String redisKey(long companyId) {
		return "employee_purchase_invite:" + companyId;
	}

	public void lockInviteCode(long companyId, String code) {
		String key = redisKey(companyId);
		Object ticketObj = stringRedisTemplate.opsForHash().get(key, code);
		Long removed = stringRedisTemplate.opsForHash().delete(key, code);
		if (removed != null && removed > 0) {
			String ticket = ticketObj == null ? null : ticketObj.toString();
			stringRedisTemplate.opsForHash().put(key, code + "_", ticket);
		} else {
			throw new ResourceException("邀请码已被使用");
		}
	}

	public void unlockInviteCode(long companyId, String code) {
		String key = redisKey(companyId);
		Object ticketObj = stringRedisTemplate.opsForHash().get(key, code + "_");
		String ticket = ticketObj == null ? null : ticketObj.toString();
		Long removed = stringRedisTemplate.opsForHash().delete(key, code + "_");
		if (removed != null && removed > 0 && ticket != null) {
			stringRedisTemplate.opsForHash().put(key, code, ticket);
		}
	}

	public void delInviteCode(long companyId, String code) {
		String key = redisKey(companyId);
		stringRedisTemplate.opsForHash().delete(key, code);
		stringRedisTemplate.opsForHash().delete(key, code + "_");
	}

	public String getTicketFieldUnderscore(long companyId, String code) {
		String key = redisKey(companyId);
		Object v = stringRedisTemplate.opsForHash().get(key, code + "_");
		return v == null ? null : v.toString();
	}

	public boolean claimInviteCodeIfAbsent(long companyId, String field, String ticket) {
		String key = redisKey(companyId);
		Object existing = stringRedisTemplate.opsForHash().get(key, field);
		if (existing == null) {
			stringRedisTemplate.opsForHash().put(key, field, ticket);
			return true;
		}
		String s = existing.toString();
		if (s.isEmpty()) {
			stringRedisTemplate.opsForHash().put(key, field, ticket);
			return true;
		}
		return false;
	}

	/**
	 * 读取未锁定邀请码对应的票据；缺失或空则视为失效。
	 */
	public String getActiveInviteTicketOrThrow(long companyId, String code) {
		if (code == null || code.isBlank()) {
			throw new ResourceException("分享链接已失效");
		}
		String key = redisKey(companyId);
		Object v = stringRedisTemplate.opsForHash().get(key, code.trim());
		if (v == null) {
			throw new ResourceException("分享链接已失效");
		}
		String ticket = v.toString();
		if (ticket == null || ticket.isEmpty()) {
			throw new ResourceException("分享链接已失效");
		}
		return ticket;
	}
}
