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

package cn.shopex.ecshopx.goods.service.hotword;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.SearchHotword;
import cn.shopex.ecshopx.goods.mapper.SearchHotwordMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-11390 F-001/F-002/F-003：热词管理端列表、详情、保存与删除。 */
@ExtendWith(MockitoExtension.class)
class SearchHotwordAdminServiceTest {

	private static final long COMPANY_ID = 11L;

	private static final long NOW = 1_800_000_000L;

	@Mock
	private SearchHotwordMapper searchHotwordMapper;

	private SearchHotwordAdminService service;

	@BeforeAll
	static void initMybatisLambdaCache() {
		// 单元测试不启动 Spring/MyBatis，LambdaUpdateWrapper 需要先建立实体字段缓存
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SearchHotword.class);
	}

	@BeforeEach
	void setUp() {
		service = new SearchHotwordAdminService(searchHotwordMapper);
	}

	private static SearchHotword row(long id, Long companyId, String content, int startTime, int endTime) {
		SearchHotword row = new SearchHotword();
		row.setId(id);
		row.setCompanyId(companyId);
		row.setContent(content);
		row.setType(SearchHotword.TYPE_TEXT);
		row.setChannel("mobile");
		row.setSort(0);
		row.setStartTime(startTime);
		row.setEndTime(endTime);
		row.setCreated((int) NOW - 100);
		return row;
	}

	private static SearchHotwordInputNormalizer.Normalized normalized() {
		return new SearchHotwordInputNormalizer.Normalized(
				"双十一",
				SearchHotword.TYPE_TEXT,
				"#000000",
				"#FFFFFF",
				null,
				null,
				Integer.valueOf(10),
				Integer.valueOf((int) NOW - 60),
				Integer.valueOf((int) NOW + 60),
				"mobile",
				null);
	}

	private static SearchHotwordAdminService.ListQuery defaultQuery() {
		return new SearchHotwordAdminService.ListQuery(null, null, null, null, null, null, null, 1L, 10L);
	}

	@Test
	void list_appliesChannelAndStatusFilters() {
		when(searchHotwordMapper.selectPage(any(), any())).thenAnswer(invocation -> {
			Page<SearchHotword> page = invocation.getArgument(0);
			page.setRecords(List.of());
			page.setTotal(0L);
			return page;
		});

		SearchHotwordAdminService.ListQuery query = new SearchHotwordAdminService.ListQuery(
				null,
				"pc",
				SearchHotwordStatus.ACTIVE,
				null,
				null,
				null,
				null,
				1L,
				10L);
		service.list(COMPANY_ID, query, NOW);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Wrapper<SearchHotword>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
		verify(searchHotwordMapper).selectPage(any(Page.class), wrapperCaptor.capture());
		String sql = wrapperCaptor.getValue().getSqlSegment();
		assertThat(sql).contains("channel");
		assertThat(sql).contains("start_time");
		assertThat(sql).contains("end_time");
	}

	@Test
	void list_returnsRowsWithComputedStatus() {
		SearchHotword active = row(1L, COMPANY_ID, "生效中热词", (int) NOW - 60, (int) NOW + 60);
		SearchHotword ended = row(2L, COMPANY_ID, "已结束热词", (int) NOW - 600, (int) NOW - 60);
		when(searchHotwordMapper.selectPage(any(), any())).thenAnswer(invocation -> {
			Page<SearchHotword> page = invocation.getArgument(0);
			page.setRecords(List.of(active, ended));
			page.setTotal(2L);
			return page;
		});

		Map<String, Object> result = service.list(COMPANY_ID, defaultQuery(), NOW);

		assertThat(result.get("total_count")).isEqualTo(2L);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
		assertThat(list).hasSize(2);
		assertThat(list.get(0).get("status")).isEqualTo(SearchHotwordStatus.ACTIVE);
		assertThat(list.get(0).get("status_name")).isEqualTo("生效中");
		assertThat(list.get(1).get("status")).isEqualTo(SearchHotwordStatus.ENDED);
		assertThat(list.get(0).get("start_time_text")).isNotNull();
	}

	@Test
	void detail_returnsNullForForeignCompany() {
		when(searchHotwordMapper.selectById(7L)).thenReturn(row(7L, 99L, "他司热词", (int) NOW, (int) NOW));

		assertThat(service.detail(COMPANY_ID, 7L, NOW)).isNull();
	}

	@Test
	void detail_returnsOwnRow() {
		when(searchHotwordMapper.selectById(7L)).thenReturn(row(7L, COMPANY_ID, "本司热词", (int) NOW, (int) NOW));

		Map<String, Object> detail = service.detail(COMPANY_ID, 7L, NOW);

		assertThat(detail).isNotNull();
		assertThat(detail.get("content")).isEqualTo("本司热词");
		assertThat(detail.get("status")).isEqualTo(SearchHotwordStatus.ACTIVE);
	}

	@Test
	void save_insertsNewRow() {
		when(searchHotwordMapper.insert(any(SearchHotword.class))).thenAnswer(invocation -> {
			SearchHotword entity = invocation.getArgument(0);
			entity.setId(99L);
			return 1;
		});

		Map<String, Object> saved = service.save(COMPANY_ID, null, normalized(), NOW);

		ArgumentCaptor<SearchHotword> captor = ArgumentCaptor.forClass(SearchHotword.class);
		verify(searchHotwordMapper).insert(captor.capture());
		SearchHotword entity = captor.getValue();
		assertThat(entity.getCompanyId()).isEqualTo(COMPANY_ID);
		assertThat(entity.getContent()).isEqualTo("双十一");
		assertThat(entity.getChannel()).isEqualTo("mobile");
		assertThat(entity.getCreated()).isEqualTo((int) NOW);
		assertThat(entity.getUpdated()).isEqualTo((int) NOW);
		assertThat(saved.get("id")).isEqualTo(99L);
	}

	@Test
	void save_updateFailsWhenRowMissing() {
		when(searchHotwordMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

		assertThatThrownBy(() -> service.save(COMPANY_ID, 7L, normalized(), NOW))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("热词不存在或已删除");
	}

	@Test
	void save_updateKeepsCreatedAndRefreshesUpdated() {
		SearchHotword fresh = row(7L, COMPANY_ID, "双十一", (int) NOW - 60, (int) NOW + 60);
		when(searchHotwordMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
		when(searchHotwordMapper.selectById(7L)).thenReturn(fresh);

		Map<String, Object> saved = service.save(COMPANY_ID, 7L, normalized(), NOW);

		assertThat(saved.get("id")).isEqualTo(7L);
		assertThat(saved.get("created")).isEqualTo((int) NOW - 100);
	}

	@Test
	void delete_reportsWhetherAnythingWasRemoved() {
		when(searchHotwordMapper.delete(any(Wrapper.class))).thenReturn(1, 0);

		assertThat(service.delete(COMPANY_ID, 7L)).isTrue();
		assertThat(service.delete(COMPANY_ID, 8L)).isFalse();
	}
}
