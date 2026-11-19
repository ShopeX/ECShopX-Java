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

package cn.shopex.ecshopx.popularize.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PopularizeBrokerageListReadServiceTest {

	@Mock
	private BrokerageMapper brokerageMapper;

	private PopularizeBrokerageListReadService service;

	@BeforeEach
	void setUp() {
		service = new PopularizeBrokerageListReadService(brokerageMapper, new ObjectMapper());
	}

	@Test
	void distributorIdZeroUsesDefaultMapper() {
		Page<Brokerage> page = new Page<>(1, 10);
		page.setTotal(0);
		when(brokerageMapper.selectPage(any(), any())).thenReturn(page);
		service.getBrokerageList(1L, "1", "10", null, null, "0");
		verify(brokerageMapper).selectPage(any(), any());
	}

	@Test
	void invalidDistributorIdThrows() {
		Assertions.assertThrows(
				BadRequestException.class,
				() -> service.getBrokerageList(1L, "1", "10", null, null, "abc"));
	}
}