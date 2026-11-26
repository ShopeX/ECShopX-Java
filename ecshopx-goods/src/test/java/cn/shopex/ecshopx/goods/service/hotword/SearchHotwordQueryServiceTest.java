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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.SearchHotword;
import cn.shopex.ecshopx.goods.mapper.SearchHotwordMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-11390 F-005：热门搜索查询的渠道过滤、完整返回与排序透传。 */
@ExtendWith(MockitoExtension.class)
class SearchHotwordQueryServiceTest {

	private static final long COMPANY_ID = 11L;

	private static final long NOW = 1_800_000_000L;

	@Mock
	private SearchHotwordMapper searchHotwordMapper;

	private SearchHotwordQueryService service;

	@BeforeEach
	void setUp() {
		service = new SearchHotwordQueryService(searchHotwordMapper);
	}

	private static SearchHotword row(long id, String content, String channel, int sort) {
		SearchHotword row = new SearchHotword();
		row.setId(id);
		row.setCompanyId(COMPANY_ID);
		row.setContent(content);
		row.setType(SearchHotword.TYPE_TEXT);
		row.setChannel(channel);
		row.setSort(sort);
		row.setStartTime((int) NOW - 60);
		row.setEndTime((int) NOW + 60);
		return row;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> listOf(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("list");
	}

	@Test
	void mobileChannel_returnsOnlyMobileRows_withoutTruncation() {
		List<SearchHotword> rows = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			rows.add(row(i, "词" + i, i % 2 == 0 ? "mobile" : "pc", 100 - i));
		}
		when(searchHotwordMapper.selectList(any())).thenReturn(rows);

		Map<String, Object> result = service.listEffective(COMPANY_ID, "mobile", NOW);

		assertThat(result.get("total_count")).isEqualTo(6L);
		List<Map<String, Object>> list = listOf(result);
		assertThat(list).hasSize(6);
		for (Map<String, Object> item : list) {
			assertThat(item.get("channel")).isEqualTo("mobile");
		}
	}

	@Test
	void pcChannel_returnsOnlyPcRows() {
		when(searchHotwordMapper.selectList(any()))
				.thenReturn(List.of(row(1L, "移动词", "mobile", 10), row(2L, "PC词", "pc", 5)));

		Map<String, Object> result = service.listEffective(COMPANY_ID, "pc", NOW);

		assertThat(result.get("total_count")).isEqualTo(1L);
		List<Map<String, Object>> list = listOf(result);
		assertThat(list).hasSize(1);
		assertThat(list.get(0).get("content")).isEqualTo("PC词");
	}

	@Test
	void channelDefaultsToMobile() {
		when(searchHotwordMapper.selectList(any())).thenReturn(List.of(row(1L, "移动词", "mobile", 10)));

		Map<String, Object> result = service.listEffective(COMPANY_ID, null, NOW);

		assertThat(result.get("total_count")).isEqualTo(1L);
	}

	@Test
	void emptyResult_returnsZeroAndEmptyList() {
		when(searchHotwordMapper.selectList(any())).thenReturn(List.of());

		Map<String, Object> result = service.listEffective(COMPANY_ID, "mobile", NOW);

		assertThat(result.get("total_count")).isEqualTo(0L);
		assertThat(listOf(result)).isEmpty();
	}

	@Test
	void invalidChannel_isRejectedBeforeQuery() {
		assertThatThrownBy(() -> service.listEffective(COMPANY_ID, "weapp", NOW))
				.isInstanceOf(BadRequestException.class);

		verify(searchHotwordMapper, never()).selectList(any());
	}
}
