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

package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MemberTaskListServiceTest {

	@Mock
	private MemberTaskMapper memberTaskMapper;

	@Mock
	private MessageSource messageSource;

	@Mock
	private MemberTaskRewardsDisplayService memberTaskRewardsDisplayService;

	private MemberTaskListService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskListService(
						memberTaskMapper, messageSource, memberTaskRewardsDisplayService);
	}

	@Test
	void list_filterTimeIncomplete_zh() {
		when(messageSource.getMessage(
						eq("promotions.member_task.filter_time_incomplete"),
						eq(null),
						eq("请同时选择开始和结束时间"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("请同时选择开始和结束时间");

		assertThatThrownBy(
						() ->
								service.list(
										1L,
										null,
										null,
										null,
										null,
										100,
										null,
										1,
										20,
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("请同时选择开始和结束时间");
	}

	@Test
	void list_filterTimeIncomplete_en() {
		when(messageSource.getMessage(
						eq("promotions.member_task.filter_time_incomplete"),
						eq(null),
						eq("请同时选择开始和结束时间"),
						eq(Locale.US)))
				.thenReturn("Please select both start and end time");

		assertThatThrownBy(
						() ->
								service.list(
										1L,
										null,
										null,
										null,
										null,
										100,
										null,
										1,
										20,
										Locale.US))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Please select both start and end time");
	}

	@Test
	void list_filterTimeInvalid_whenBeginAfterEnd() {
		when(messageSource.getMessage(
						eq("promotions.member_task.filter_time_invalid"),
						eq(null),
						eq("结束时间必须晚于开始时间"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("结束时间必须晚于开始时间");

		assertThatThrownBy(
						() ->
								service.list(
										1L,
										null,
										null,
										null,
										null,
										200,
										100,
										1,
										20,
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("结束时间必须晚于开始时间");
	}

	@Test
	void list_displayStatusInvalid() {
		when(messageSource.getMessage(
						eq("promotions.member_task.display_status_invalid"),
						eq(null),
						eq("任务状态无效"),
						eq(Locale.SIMPLIFIED_CHINESE)))
				.thenReturn("任务状态无效");

		assertThatThrownBy(
						() ->
								service.list(
										1L,
										null,
										null,
										null,
										"bogus",
										null,
										null,
										1,
										20,
										Locale.SIMPLIFIED_CHINESE))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("任务状态无效");
	}

	@Test
	void list_empty_returnsZeroTotal() {
		when(memberTaskMapper.countAdminList(
						eq(1L),
						isNull(),
						isNull(),
						isNull(),
						isNull(),
						isNull(),
						isNull(),
						anyLong()))
				.thenReturn(0L);

		Map<String, Object> out =
				service.list(1L, null, null, null, null, null, null, 1, 20, Locale.SIMPLIFIED_CHINESE);

		assertThat(out.get("total_count")).isEqualTo(0L);
		assertThat(out.get("list")).isEqualTo(List.of());
		assertThat(out.get("page")).isEqualTo(1);
		assertThat(out.get("page_size")).isEqualTo(20);
	}
}
