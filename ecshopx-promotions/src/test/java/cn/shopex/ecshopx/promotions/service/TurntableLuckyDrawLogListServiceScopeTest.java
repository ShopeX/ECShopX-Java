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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

@ExtendWith(MockitoExtension.class)
class TurntableLuckyDrawLogListServiceScopeTest {

	@Mock TurntableLogMapper turntableLogMapper;
	@Mock MemberAccountService memberAccountService;

	TurntableLuckyDrawLogListService service;

	@BeforeAll
	static void initTableInfo() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), TurntableLog.class);
	}

	@BeforeEach
	void setUp() {
		service = new TurntableLuckyDrawLogListService(turntableLogMapper, memberAccountService);
	}

	@Test
	void getLogStatistics_foreignActId_returnsEmptyWithoutPaging() {
		when(turntableLogMapper.countAllByActId(100L, 42L)).thenReturn(0L);

		Map<String, Object> out = service.getLogStatistics(100L, 42L, "1", "20");

		assertThat(out.get("total_count")).isEqualTo(0L);
		assertThat(out.get("list")).isEqualTo(List.of());
		verify(turntableLogMapper, never()).selectPage(any(), any());
	}

	@Test
	void getLogStatistics_ownCompany_pageQueryIncludesCompanyId() {
		when(turntableLogMapper.countAllByActId(100L, 42L)).thenReturn(1L);
		when(turntableLogMapper.selectPage(any(), any())).thenAnswer(inv -> inv.getArgument(0));

		service.getLogStatistics(100L, 42L, "1", "20");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<TurntableLog>> cap =
				ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(turntableLogMapper).selectPage(any(), cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("company_id");
	}
}
