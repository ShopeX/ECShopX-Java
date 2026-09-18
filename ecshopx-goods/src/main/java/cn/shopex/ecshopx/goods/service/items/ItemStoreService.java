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

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.dispatch.ItemStoreUpdatedEventBusPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ItemStoreService {

	private final StringRedisTemplate redisTemplate;
	private final ItemWarningStoreRedisAccessor itemWarningStoreRedisAccessor;
	private final ItemsMapper itemsMapper;
	private final DistributorItemsMapper distributorItemsMapper;
	private final ItemStoreUpdatedEventBusPublisher itemStoreUpdatedEventBusPublisher;

	public ItemStoreService(@Qualifier("companysRedisTemplate") StringRedisTemplate redisTemplate,
			ItemWarningStoreRedisAccessor itemWarningStoreRedisAccessor,
			ItemsMapper itemsMapper,
			DistributorItemsMapper distributorItemsMapper,
			ItemStoreUpdatedEventBusPublisher itemStoreUpdatedEventBusPublisher) {
		this.redisTemplate = redisTemplate;
		this.itemWarningStoreRedisAccessor = itemWarningStoreRedisAccessor;
		this.itemsMapper = itemsMapper;
		this.distributorItemsMapper = distributorItemsMapper;
		this.itemStoreUpdatedEventBusPublisher = itemStoreUpdatedEventBusPublisher;
	}

	/**
	 * Decrements (or increments when {@code num} is negative) Redis {@code item_store:*} and writes the resulting store
	 * to {@code items} or {@code distributor_items}.
	 *
	 * <p>{@code writeDistributorItems=true} 时 Redis 带店前缀且回写 {@code distributor_items}；否则扣 {@code item_store:{itemId}} 并回写
	 * {@code items}。
	 *
	 * @param redisDistributorId    &gt;0 时 Redis key 为 {@code {id}_{itemId}}，否则为 {@code {itemId}}
	 * @param writeDistributorItems true 时回写 {@code distributor_items}（需 {@code redisDistributorId > 0}），否则回写 {@code items}
	 */
	public boolean minusItemStore(
			long itemId, int num, long redisDistributorId, boolean writeDistributorItems, long companyId) {
		String keySuffix = redisDistributorId > 0L ? redisDistributorId + "_" + itemId : String.valueOf(itemId);
		String redisKey = "item_store:" + keySuffix;
		Long newVal = redisTemplate.opsForValue().increment(redisKey, -num);
		if (newVal != null && newVal < 0L) {
			redisTemplate.opsForValue().increment(redisKey, num);
			return false;
		}
		long store = newVal != null ? newVal : 0L;
		int storeInt = (int) Math.min(store, Integer.MAX_VALUE);
		if (writeDistributorItems && redisDistributorId > 0L) {
			distributorItemsMapper.update(null, new LambdaUpdateWrapper<DistributorItems>()
					.eq(DistributorItems::getCompanyId, companyId)
					.eq(DistributorItems::getDistributorId, redisDistributorId)
					.eq(DistributorItems::getItemId, itemId)
					.set(DistributorItems::getStore, (long) storeInt));
		} else {
			itemsMapper.update(null, new LambdaUpdateWrapper<Items>()
					.eq(Items::getCompanyId, companyId)
					.eq(Items::getItemId, itemId)
					.set(Items::getStore, storeInt));
		}
		return true;
	}

	/**
	 * 兼容旧语义：{@code isTotalStore=false} 且 {@code distributorId>0} 时扣店铺关联库存并回写
	 * {@code distributor_items}；否则扣平台 key 并回写 {@code items}。
	 */
	public boolean minusItemStoreByTotalStoreFlag(
			long itemId, int num, long distributorId, boolean isTotalStore, long companyId) {
		boolean writeDistributorItems = distributorId > 0L && !isTotalStore;
		long redisDistributorId = writeDistributorItems ? distributorId : 0L;
		return minusItemStore(itemId, num, redisDistributorId, writeDistributorItems, companyId);
	}

	public int getPlatformWarningStore(long companyId) {
		return itemWarningStoreRedisAccessor.getPlatformWarningStore(companyId);
	}

	public int getSupplierWarningStore(long companyId, long supplierId) {
		return itemWarningStoreRedisAccessor.getSupplierWarningStore(companyId, supplierId);
	}

	public int getDistributorWarningStore(long companyId, long distributorId) {
		return itemWarningStoreRedisAccessor.getDistributorWarningStore(companyId, distributorId);
	}

	public void saveItemStore(long itemId, int store, long distributorId) {
		String key = redisKey(itemId, distributorId);
		redisTemplate.opsForValue().set("item_store:" + key, String.valueOf(store));
		itemStoreUpdatedEventBusPublisher.publish(itemId, store, distributorId);
	}

	/**
	 * Wdt inventory sync ordering: notify subscribers on the bus first, then write Redis (always true for cache write).
	 */
	public boolean saveItemStoreForWdtSync(long itemId, int store, long distributorId) {
		itemStoreUpdatedEventBusPublisher.publish(itemId, store, distributorId);
		String key = redisKey(itemId, distributorId);
		redisTemplate.opsForValue().set("item_store:" + key, String.valueOf(store));
		return true;
	}

	public void deleteItemStore(long itemId, long distributorId) {
		String key = redisKey(itemId, distributorId);
		redisTemplate.delete("item_store:" + key);
	}

	private static String redisKey(long itemId, long distributorId) {
		if (distributorId > 0) {
			return distributorId + "_" + itemId;
		}
		return String.valueOf(itemId);
	}

	public String nextBn(String prefix) {
		String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
		String counterKey = prefix + "_counter_date:" + today;
		Long n = redisTemplate.opsForValue().increment(counterKey);
		if (n != null && n == 1L) {
			redisTemplate.expire(counterKey, 86401, TimeUnit.SECONDS);
		}
		long seq = n != null ? n : 1L;
		return prefix + today + String.format("%08d", seq);
	}
}
