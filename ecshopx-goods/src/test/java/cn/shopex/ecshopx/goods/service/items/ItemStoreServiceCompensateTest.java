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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.dispatch.ItemStoreUpdatedEventBusPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ItemStoreServiceCompensateTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, Items.class);
		TableInfoHelper.initTableInfo(assistant, DistributorItems.class);
	}

	@Mock private StringRedisTemplate redisTemplate;
	@Mock private ValueOperations<String, String> valueOps;
	@Mock private ItemWarningStoreRedisAccessor itemWarningStoreRedisAccessor;
	@Mock private ItemsMapper itemsMapper;
	@Mock private DistributorItemsMapper distributorItemsMapper;
	@Mock private ItemStoreUpdatedEventBusPublisher itemStoreUpdatedEventBusPublisher;

	private ItemStoreService service;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		service =
				new ItemStoreService(
						redisTemplate,
						itemWarningStoreRedisAccessor,
						itemsMapper,
						distributorItemsMapper,
						itemStoreUpdatedEventBusPublisher);
	}

	@Test
	@DisplayName("items 更新影响行数为 0 时回补 Redis 并返回 false")
	void minus_whenItemsUpdateZero_compensatesRedis() {
		when(valueOps.increment(eq("item_store:16"), eq(-2L))).thenReturn(8L);
		when(itemsMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		boolean ok = service.minusItemStore(16L, 2, 0L, false, 1L);

		assertThat(ok).isFalse();
		verify(valueOps).increment(eq("item_store:16"), eq(2L));
		verify(distributorItemsMapper, never()).update(isNull(), any());
	}

	@Test
	@DisplayName("distributor_items 更新影响行数为 0 时回补 Redis 并返回 false")
	void minus_whenDistributorItemsUpdateZero_compensatesRedis() {
		when(valueOps.increment(eq("item_store:3008_16"), eq(-1L))).thenReturn(5L);
		when(distributorItemsMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

		boolean ok = service.minusItemStore(16L, 1, 3008L, true, 1L);

		assertThat(ok).isFalse();
		verify(valueOps).increment(eq("item_store:3008_16"), eq(1L));
		verify(itemsMapper, never()).update(isNull(), any());
	}

	@Test
	@DisplayName("DB 更新成功时不回补 Redis")
	void minus_whenDbUpdateOk_doesNotCompensate() {
		when(valueOps.increment(eq("item_store:16"), eq(-3L))).thenReturn(7L);
		when(itemsMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		boolean ok = service.minusItemStore(16L, 3, 0L, false, 1L);

		assertThat(ok).isTrue();
		verify(valueOps, never()).increment(eq("item_store:16"), eq(3L));
	}
}
