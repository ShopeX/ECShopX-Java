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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.dispatch.ItemStoreUpdatedEventBusPublisher;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * ECX-11447: sync must not raise an existing item_store Redis value above the live sellable qty.
 */
@ExtendWith(MockitoExtension.class)
class ItemStoreServiceGuardedSaveTest {

	@Mock private StringRedisTemplate redisTemplate;
	@Mock private ValueOperations<String, String> valueOps;
	@Mock private ItemWarningStoreRedisAccessor itemWarningStoreRedisAccessor;
	@Mock private ItemsMapper itemsMapper;
	@Mock private DistributorItemsMapper distributorItemsMapper;
	@Mock private ItemStoreUpdatedEventBusPublisher itemStoreUpdatedEventBusPublisher;

	private ItemStoreService service;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
		service =
				new ItemStoreService(
						redisTemplate,
						itemWarningStoreRedisAccessor,
						itemsMapper,
						distributorItemsMapper,
						itemStoreUpdatedEventBusPublisher);
	}

	@Test
	@DisplayName("saveItemStore skips Redis write when proposed store is higher than current")
	void saveItemStore_doesNotRaiseExistingRedis() {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("10"))).thenReturn(0L);

		service.saveItemStore(16L, 10, 0L);

		verify(itemStoreUpdatedEventBusPublisher, never()).publish(eq(16L), eq(10), eq(0L));
		verify(valueOps, never()).set(eq("item_store:16"), anyString());
	}

	@Test
	@DisplayName("saveItemStore writes and publishes when guarded script accepts the value")
	void saveItemStore_writesWhenScriptAllows() {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("10"))).thenReturn(1L);

		service.saveItemStore(16L, 10, 0L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
		verify(redisTemplate).execute(scriptCaptor.capture(), anyList(), eq("10"));
		assertThat(scriptCaptor.getValue()).isInstanceOf(DefaultRedisScript.class);
		verify(itemStoreUpdatedEventBusPublisher).publish(16L, 10, 0L);
	}

	@Test
	@DisplayName("saveItemStoreForWdtSync returns false and skips publish when raise would resurrect stock")
	void saveItemStoreForWdtSync_returnsFalseWhenRaiseBlocked() {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("10"))).thenReturn(0L);

		boolean ok = service.saveItemStoreForWdtSync(16L, 10, 0L);

		assertThat(ok).isFalse();
		verify(itemStoreUpdatedEventBusPublisher, never()).publish(eq(16L), eq(10), eq(0L));
	}

	@Test
	@DisplayName("overwriteItemStore always sets Redis and publishes")
	void overwriteItemStore_forceSetsEvenWhenHigher() {
		service.overwriteItemStore(16L, 20, 0L);

		verify(valueOps).set("item_store:16", "20");
		verify(itemStoreUpdatedEventBusPublisher).publish(16L, 20, 0L);
	}
}
