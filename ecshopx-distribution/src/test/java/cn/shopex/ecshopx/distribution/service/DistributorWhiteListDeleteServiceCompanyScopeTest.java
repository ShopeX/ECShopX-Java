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

package cn.shopex.ecshopx.distribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** ECX-10885：白名单删除的查询和删除都必须限定当前租户。 */
class DistributorWhiteListDeleteServiceCompanyScopeTest {

	private DistributorWhiteListMapper mapper;
	private DistributorWhiteListDeleteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DistributorWhiteList.class);
	}

	@BeforeEach
	void setUp() {
		mapper = org.mockito.Mockito.mock(DistributorWhiteListMapper.class);
		service = new DistributorWhiteListDeleteService(mapper);
	}

	@Test
	void deleteByWhiteListId_scopesLookupAndMobileDeleteByCompany() {
		DistributorWhiteList row = new DistributorWhiteList();
		row.setId(12L);
		row.setCompanyId(100L);
		row.setMobile("13800000000");
		when(mapper.selectList(any())).thenReturn(List.of(row));

		service.deleteWhiteList(100L, true, 12L, "staff", null);

		assertThat(capturedSelect().getSqlSegment()).containsIgnoringCase("company_id");
		assertThat(capturedDelete().getSqlSegment()).containsIgnoringCase("company_id");
	}

	@Test
	void deleteByWhiteListId_doesNotDeleteWhenCompanyLookupMisses() {
		when(mapper.selectList(any())).thenReturn(List.of());

		service.deleteWhiteList(100L, true, 99L, "staff", null);

		assertThat(capturedSelect().getSqlSegment()).containsIgnoringCase("company_id");
		verify(mapper, never()).delete(any());
	}

	@Test
	void deleteByDistributorId_scopesDeleteByCompany() {
		service.deleteWhiteList(100L, false, 7L, "staff", null);

		assertThat(capturedDelete().getSqlSegment()).containsIgnoringCase("company_id");
	}

	@SuppressWarnings("unchecked")
	private Wrapper<DistributorWhiteList> capturedSelect() {
		ArgumentCaptor<Wrapper<DistributorWhiteList>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(mapper).selectList(captor.capture());
		return captor.getValue();
	}

	@SuppressWarnings("unchecked")
	private Wrapper<DistributorWhiteList> capturedDelete() {
		ArgumentCaptor<Wrapper<DistributorWhiteList>> captor = ArgumentCaptor.forClass(Wrapper.class);
		verify(mapper).delete(captor.capture());
		return captor.getValue();
	}
}
