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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskTypeCatalog;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberTaskListService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final DateTimeFormatter TASK_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberTaskMapper memberTaskMapper;
	private final MessageSource messageSource;
	private final MemberTaskRewardsDisplayService memberTaskRewardsDisplayService;

	public MemberTaskListService(
			MemberTaskMapper memberTaskMapper,
			MessageSource messageSource,
			MemberTaskRewardsDisplayService memberTaskRewardsDisplayService) {
		this.memberTaskMapper = memberTaskMapper;
		this.messageSource = messageSource;
		this.memberTaskRewardsDisplayService = memberTaskRewardsDisplayService;
	}

	public Map<String, Object> list(
			long companyId,
			String taskIdKeyword,
			String nameKeyword,
			String typeDetail,
			String displayStatus,
			Integer filterBegin,
			Integer filterEnd,
			int page,
			int pageSize,
			Locale locale) {
		validateFilters(filterBegin, filterEnd, displayStatus, locale);
		int safePage = Math.max(1, page);
		int safePageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
		String statusFilter =
				StringUtils.hasText(displayStatus) ? displayStatus.trim() : null;

		long now = System.currentTimeMillis() / 1000L;
		long total =
				memberTaskMapper.countAdminList(
						companyId,
						trimToNull(taskIdKeyword),
						trimToNull(nameKeyword),
						trimToNull(typeDetail),
						statusFilter,
						filterBegin,
						filterEnd,
						now);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("page", safePage);
		out.put("page_size", safePageSize);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}

		long offset = (long) (safePage - 1) * (long) safePageSize;
		List<MemberTask> rows =
				memberTaskMapper.selectAdminListPage(
						companyId,
						trimToNull(taskIdKeyword),
						trimToNull(nameKeyword),
						trimToNull(typeDetail),
						statusFilter,
						filterBegin,
						filterEnd,
						now,
						offset,
						safePageSize);

		List<Map<String, Object>> list = new ArrayList<>();
		for (MemberTask row : rows) {
			list.add(toListItem(row, now));
		}
		out.put("list", list);
		return out;
	}

	private void validateFilters(
			Integer filterBegin, Integer filterEnd, String displayStatus, Locale locale) {
		boolean hasBegin = filterBegin != null && filterBegin > 0;
		boolean hasEnd = filterEnd != null && filterEnd > 0;
		if (hasBegin != hasEnd) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_task.filter_time_incomplete",
							null,
							"请同时选择开始和结束时间",
							locale));
		}
		if (hasBegin && filterBegin > filterEnd) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_task.filter_time_invalid",
							null,
							"结束时间必须晚于开始时间",
							locale));
		}
		if (StringUtils.hasText(displayStatus)
				&& !MemberTaskDisplayStatus.isKnownFilter(displayStatus.trim())) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.member_task.display_status_invalid",
							null,
							"任务状态无效",
							locale));
		}
	}

	private static String trimToNull(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		return raw.trim();
	}

	private Map<String, Object> toListItem(MemberTask row, long now) {
		int published = row.getPublished() != null ? row.getPublished() : 0;
		int offShelf = row.getOffShelf() != null ? row.getOffShelf() : 0;
		int begin = row.getBeginTime() != null ? row.getBeginTime() : 0;
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		String category = row.getTypeCategory() != null ? row.getTypeCategory() : "";
		String detail = row.getTypeDetail() != null ? row.getTypeDetail() : "";

		Map<String, Object> item = new LinkedHashMap<>();
		item.put("task_id", row.getTaskId());
		item.put("name", row.getName());
		item.put("type_category", category);
		item.put("type_detail", detail);
		item.put("type_label", MemberTaskTypeCatalog.typeLabel(category, detail));
		long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
		item.put(
				"rewards",
				memberTaskRewardsDisplayService.buildRewards(companyId, row.getRewardConfig()));
		item.put(
				"display_status",
				MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, now));
		item.put("begin_time", begin > 0 ? begin : null);
		item.put("end_time", end > 0 ? end : null);
		item.put("begin_time_text", begin > 0 ? formatEpoch(begin) : "");
		item.put("end_time_text", end > 0 ? formatEpoch(end) : "");
		return item;
	}

	static String formatEpoch(int epochSec) {
		if (epochSec <= 0) {
			return "";
		}
		return TASK_TIME_FMT.format(Instant.ofEpochSecond(epochSec));
	}
}
