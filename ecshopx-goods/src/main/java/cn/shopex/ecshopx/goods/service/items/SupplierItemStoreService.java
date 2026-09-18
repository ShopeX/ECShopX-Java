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

import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.mapper.SupplierItemsMapper;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SupplierItemStoreService {

	private final StringRedisTemplate redisTemplate;
	private final SupplierItemsRepository supplierItemsRepository;
	private final SupplierItemsMapper supplierItemsMapper;

	public SupplierItemStoreService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redisTemplate,
			SupplierItemsRepository supplierItemsRepository,
			SupplierItemsMapper supplierItemsMapper) {
		this.redisTemplate = redisTemplate;
		this.supplierItemsRepository = supplierItemsRepository;
		this.supplierItemsMapper = supplierItemsMapper;
	}

	public void saveSupplierItemStore(long supplierItemId, int store) {
		if (supplierItemId <= 0L) {
			return;
		}
		redisTemplate.opsForValue().set(redisKey(supplierItemId), String.valueOf(store));
	}

	public void deleteSupplierItemStore(long supplierItemId) {
		if (supplierItemId <= 0L) {
			return;
		}
		redisTemplate.delete(redisKey(supplierItemId));
	}

	/**
	 * Decrements (or increments when {@code num} is negative) Redis {@code supplier_item_store:*}
	 * and writes the resulting store to {@code supplier_items}.
	 */
	public boolean minusSupplierItemStore(long supplierItemId, int num, long companyId) {
		if (supplierItemId <= 0L || companyId <= 0L) {
			return false;
		}
		String redisKey = redisKey(supplierItemId);
		Long newVal = redisTemplate.opsForValue().increment(redisKey, -num);
		if (newVal != null && newVal < 0L) {
			redisTemplate.opsForValue().increment(redisKey, num);
			return false;
		}
		long store = newVal != null ? newVal : 0L;
		int storeInt = (int) Math.min(store, Integer.MAX_VALUE);
		supplierItemsMapper.update(
				null,
				new LambdaUpdateWrapper<SupplierItems>()
						.eq(SupplierItems::getCompanyId, companyId)
						.eq(SupplierItems::getItemId, supplierItemId)
						.set(SupplierItems::getStore, storeInt));
		return true;
	}

	public int getStoreFromDb(long companyId, long supplierItemId) {
		if (companyId <= 0L || supplierItemId <= 0L) {
			return 0;
		}
		SupplierItems row = supplierItemsRepository.getByItemIdAndCompany(supplierItemId, companyId);
		if (row == null || row.getStore() == null) {
			return 0;
		}
		return row.getStore();
	}

	public int resolveSupplierItemStore(long companyId, long supplierItemId) {
		if (supplierItemId <= 0L) {
			return 0;
		}
		SupplierItems row = companyId > 0L ? supplierItemsRepository.getByItemIdAndCompany(supplierItemId, companyId) : null;
		if (row != null && (row.getIsMarket() == null || row.getIsMarket() != 1)) {
			return 0;
		}
		String redisKey = redisKey(supplierItemId);
		String cached = redisTemplate.opsForValue().get(redisKey);
		if (cached != null && !cached.isBlank()) {
			try {
				return Integer.parseInt(cached.trim());
			} catch (NumberFormatException ignored) {
				// fall through to DB
			}
		}
		if (row != null) {
			return row.getStore() != null ? row.getStore() : 0;
		}
		return getStoreFromDb(companyId, supplierItemId);
	}

	private static String redisKey(long supplierItemId) {
		return "supplier_item_store:" + supplierItemId;
	}
}
