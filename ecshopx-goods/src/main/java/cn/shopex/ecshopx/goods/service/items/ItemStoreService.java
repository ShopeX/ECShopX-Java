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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class ItemStoreService {

	/**
	 * ECX-11447: refuse to raise an existing {@code item_store} value (stale DB/sync must not resurrect sold stock).
	 * Returns 1 when SET applied, 0 when skipped because proposed &gt; current.
	 */
	private static final DefaultRedisScript<Long> GUARDED_SET_SCRIPT = new DefaultRedisScript<>();

	static {
		GUARDED_SET_SCRIPT.setResultType(Long.class);
		GUARDED_SET_SCRIPT.setScriptText(
				"local cur = redis.call('GET', KEYS[1]) "
						+ "if cur and tonumber(cur) and tonumber(ARGV[1]) > tonumber(cur) then return 0 end "
						+ "redis.call('SET', KEYS[1], ARGV[1]) return 1");
	}

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
		int rows;
		try {
			if (writeDistributorItems && redisDistributorId > 0L) {
				rows =
						distributorItemsMapper.update(
								null,
								new LambdaUpdateWrapper<DistributorItems>()
										.eq(DistributorItems::getCompanyId, companyId)
										.eq(DistributorItems::getDistributorId, redisDistributorId)
										.eq(DistributorItems::getItemId, itemId)
										.set(DistributorItems::getStore, (long) storeInt));
			} else {
				rows =
						itemsMapper.update(
								null,
								new LambdaUpdateWrapper<Items>()
										.eq(Items::getCompanyId, companyId)
										.eq(Items::getItemId, itemId)
										.set(Items::getStore, storeInt));
			}
		} catch (RuntimeException e) {
			redisTemplate.opsForValue().increment(redisKey, num);
			throw e;
		}
		if (rows < 1) {
			redisTemplate.opsForValue().increment(redisKey, num);
			return false;
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

	/**
	 * Seeds or aligns Redis store without raising an existing value (ECX-11447). Skips publish when write is refused.
	 */
	public void saveItemStore(long itemId, int store, long distributorId) {
		if (!guardedSetItemStore(itemId, store, distributorId)) {
			return;
		}
		itemStoreUpdatedEventBusPublisher.publish(itemId, store, distributorId);
	}

	/**
	 * Wdt inventory sync: write Redis only when it would not raise live sellable qty; publish only after a successful write.
	 *
	 * @return false when Redis already holds a lower value than {@code store} (raise blocked)
	 */
	public boolean saveItemStoreForWdtSync(long itemId, int store, long distributorId) {
		if (!guardedSetItemStore(itemId, store, distributorId)) {
			return false;
		}
		itemStoreUpdatedEventBusPublisher.publish(itemId, store, distributorId);
		return true;
	}

	/**
	 * Authoritative overwrite (admin / confirmed external warehouse). May raise Redis; use only when the caller owns the
	 * absolute stock figure.
	 */
	public void overwriteItemStore(long itemId, int store, long distributorId) {
		String key = redisKey(itemId, distributorId);
		redisTemplate.opsForValue().set("item_store:" + key, String.valueOf(store));
		itemStoreUpdatedEventBusPublisher.publish(itemId, store, distributorId);
	}

	public void deleteItemStore(long itemId, long distributorId) {
		String key = redisKey(itemId, distributorId);
		redisTemplate.delete("item_store:" + key);
	}

	private boolean guardedSetItemStore(long itemId, int store, long distributorId) {
		String fullKey = "item_store:" + redisKey(itemId, distributorId);
		Long applied =
				redisTemplate.execute(
						GUARDED_SET_SCRIPT, List.of(fullKey), String.valueOf(store));
		return applied != null && applied.longValue() == 1L;
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
