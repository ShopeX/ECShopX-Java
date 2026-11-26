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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.ItemsCommission;
import cn.shopex.ecshopx.goods.mapper.ItemsCommissionMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ECX-11444: set_commission_ratio stores percent×100; list display must return percent so admin
 * refill stays within 0–100 validation.
 */
@ExtendWith(MockitoExtension.class)
class ItemsCommissionQueryRepositoryPercentDisplayTest {

	@Mock private ItemsCommissionMapper mapper;

	private ItemsCommissionQueryRepository repository;

	@BeforeEach
	void setUp() {
		repository = new ItemsCommissionQueryRepository(mapper);
	}

	@Test
	@DisplayName("stored 555 (5.55%×100) maps to display 5.55")
	void mapCommissionRatioByGoodsIds_dividesStoredScaledPercentBy100() {
		ItemsCommission row = new ItemsCommission();
		row.setRelId(9001L);
		row.setCompanyId(141L);
		row.setType("goods");
		row.setCommissionConf("{\"commission\":555}");
		when(mapper.selectList(any())).thenReturn(List.of(row));

		Map<Long, Number> ratios = repository.mapCommissionRatioByGoodsIds(141L, List.of(9001L));

		assertThat(ratios).containsKey(9001L);
		assertThat(new BigDecimal(ratios.get(9001L).toString()))
				.isEqualByComparingTo(new BigDecimal("5.55"));
	}

	@Test
	@DisplayName("missing conf maps to 0")
	void mapCommissionRatioByGoodsIds_missingConf_returnsZero() {
		ItemsCommission row = new ItemsCommission();
		row.setRelId(9002L);
		row.setCompanyId(141L);
		row.setType("goods");
		row.setCommissionConf(null);
		when(mapper.selectList(any())).thenReturn(List.of(row));

		Map<Long, Number> ratios = repository.mapCommissionRatioByGoodsIds(141L, List.of(9002L));

		assertThat(new BigDecimal(ratios.get(9002L).toString())).isEqualByComparingTo(BigDecimal.ZERO);
	}
}
