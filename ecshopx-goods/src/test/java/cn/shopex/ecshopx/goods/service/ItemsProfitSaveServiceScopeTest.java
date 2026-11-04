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

package cn.shopex.ecshopx.goods.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributionConfigRedisReadService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsProfitSaveServiceScopeTest {

	@Mock ItemsProfitRepository itemsProfitRepository;
	@Mock ItemsRepository itemsRepository;
	@Mock ItemsCategoryProfitRepository itemsCategoryProfitRepository;
	@Mock DistributionConfigRedisReadService distributionConfigRedisReadService;

	ItemsProfitSaveService service;

	@BeforeEach
	void setUp() {
		service =
				new ItemsProfitSaveService(
						new ObjectMapper(),
						itemsProfitRepository,
						itemsRepository,
						itemsCategoryProfitRepository,
						distributionConfigRedisReadService);
	}

	@Test
	void save_foreignItemId_rejectsEntireEntry_withoutProfitWriteOrItemsUpdate() {
		when(itemsRepository.getByItemIdAndCompany(42L, 100L)).thenReturn(null);

		String profitConf =
				"[{\"item_id\":42,\"profit_type\":2,\"profit_conf_profit\":10,\"profit_conf_popularize_profit\":5}]";

		assertThatThrownBy(() -> service.save(100L, Map.of("profit_conf", profitConf)))
				.isInstanceOf(ResourceException.class)
				.hasMessage("商品不存在");

		verify(itemsProfitRepository, never()).deleteByCompanyAndItemIds(anyLong(), any());
		verify(itemsProfitRepository, never()).insertRow(any());
		verify(itemsRepository, never())
				.updateProfitFeeByItemId(anyLong(), anyLong(), anyInt(), anyInt());
		verify(itemsRepository, never())
				.updateProfitByItemId(anyLong(), anyLong(), anyInt(), anyString());
	}

	@Test
	void save_ownCompanyCustomProfit_updatesItemsWithCompanyScope() {
		Items owned = new Items();
		owned.setItemId(42L);
		owned.setCompanyId(100L);
		when(itemsRepository.getByItemIdAndCompany(42L, 100L)).thenReturn(owned);
		when(itemsProfitRepository.insertRow(any())).thenReturn(1);

		String profitConf =
				"[{\"item_id\":42,\"profit_type\":2,\"profit_conf_profit\":10,\"profit_conf_popularize_profit\":5}]";

		service.save(100L, Map.of("profit_conf", profitConf));

		verify(itemsRepository).updateProfitFeeByItemId(eq(100L), eq(42L), eq(3), eq(5));
	}
}
