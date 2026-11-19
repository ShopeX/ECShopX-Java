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

package cn.shopex.ecshopx.goods.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ItemsListQueryRepositoryDistributorScopeSqlTest {

	@Test
	void distributorRelVirtualExistsSql_includesStoreOwnedOrShelfCanSale() {
		String sql = ItemsListQueryRepository.distributorRelVirtualExistsSql(141L, 3361L, Boolean.TRUE);

		assertThat(sql).contains("items.distributor_id = 3361");
		assertThat(sql).contains("OR EXISTS (SELECT 1 FROM distribution_distributor_items d");
		assertThat(sql).contains("d.company_id = 141");
		assertThat(sql).contains("d.distributor_id = 3361");
		assertThat(sql).contains("d.goods_can_sale = 1");
	}

	@Test
	void distributorRelVirtualExistsSql_omitsGoodsCanSaleWhenNull() {
		String sql = ItemsListQueryRepository.distributorRelVirtualExistsSql(1L, 9L, null);

		assertThat(sql).contains("items.distributor_id = 9");
		assertThat(sql).doesNotContain("goods_can_sale");
	}
}
