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

package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TurntableLogCountServiceScopeTest {

	@Mock TurntableLogMapper turntableLogMapper;

	TurntableLogCountService service;

	@BeforeEach
	void setUp() {
		service = new TurntableLogCountService(turntableLogMapper);
	}

	@Test
	void getLogCount_passesCompanyIdToAllMapperCounts() {
		when(turntableLogMapper.countAllByActId(100L, 42L)).thenReturn(3L);
		when(turntableLogMapper.countDistinctUserByActId(100L, 42L)).thenReturn(2L);
		when(turntableLogMapper.countNonThanksByActId(100L, 42L)).thenReturn(1L);
		when(turntableLogMapper.countDistinctUserNonThanksByActId(100L, 42L)).thenReturn(1L);

		Map<String, Object> out = service.getLogCount(100L, 42L);

		assertThat(out.get("total")).isEqualTo(3L);
		verify(turntableLogMapper).countAllByActId(100L, 42L);
		verify(turntableLogMapper).countDistinctUserByActId(100L, 42L);
		verify(turntableLogMapper).countNonThanksByActId(100L, 42L);
		verify(turntableLogMapper).countDistinctUserNonThanksByActId(100L, 42L);
	}
}
