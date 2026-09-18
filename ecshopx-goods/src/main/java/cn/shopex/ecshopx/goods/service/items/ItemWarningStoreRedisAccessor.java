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

package cn.shopex.ecshopx.goods.service.items;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ItemWarningStoreRedisAccessor {

	private static final int DEFAULT_WARNING = 5;

	private final StringRedisTemplate companysRedisTemplate;

	public ItemWarningStoreRedisAccessor(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public int getPlatformWarningStore(long companyId) {
		String key = "item_warning_store:" + companyId;
		String v = companysRedisTemplate.opsForValue().get(key);
		return parseIntOrDefault(v, DEFAULT_WARNING);
	}

	public int getSupplierWarningStore(long companyId, long supplierId) {
		String key = "supplier_warning_store:" + companyId + ":" + supplierId;
		String v = companysRedisTemplate.opsForValue().get(key);
		return parseIntOrDefault(v, DEFAULT_WARNING);
	}

	/**
	 * Per-store low-stock warning threshold from Redis. Key format: {@code item_warning_store:{companyId}{distributorId}} (no separator between ids).
	 */
	public int getDistributorWarningStore(long companyId, long distributorId) {
		String key = "item_warning_store:" + companyId + distributorId;
		String v = companysRedisTemplate.opsForValue().get(key);
		return parseIntOrDefault(v, DEFAULT_WARNING);
	}

	public void setPlatformWarningStore(long companyId, int store) {
		String key = "item_warning_store:" + companyId;
		companysRedisTemplate.opsForValue().set(key, String.valueOf(store));
	}

	public void setDistributorWarningStore(long companyId, long distributorId, int store) {
		String key = "item_warning_store:" + companyId + distributorId;
		companysRedisTemplate.opsForValue().set(key, String.valueOf(store));
	}

	public void setSupplierWarningStore(long companyId, long supplierId, int store) {
		String key = "supplier_warning_store:" + companyId + ":" + supplierId;
		companysRedisTemplate.opsForValue().set(key, String.valueOf(store));
	}

	private static int parseIntOrDefault(String v, int d) {
		if (!StringUtils.hasText(v)) {
			return d;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return d;
		}
	}
}
