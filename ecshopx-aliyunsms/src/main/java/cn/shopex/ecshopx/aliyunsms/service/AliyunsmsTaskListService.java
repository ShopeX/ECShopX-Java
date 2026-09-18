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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTaskListService {

	private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FMT_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final TaskMapper taskMapper;

	public AliyunsmsTaskListService(TaskMapper taskMapper) {
		this.taskMapper = taskMapper;
	}

	public Map<String, Object> getList(long companyId, TaskListQuery q) {
		LambdaQueryWrapper<Task> whereOnly = baseWhereWrapper(companyId, q);
		long total = taskMapper.selectCount(whereOnly);
		if (total == 0) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		LambdaQueryWrapper<Task> listW = baseWhereWrapper(companyId, q);
		listW.select(
				Task::getId,
				Task::getTaskName,
				Task::getTemplateName,
				Task::getSendAt,
				Task::getTotalNum,
				Task::getFailedNum,
				Task::getStatus,
				Task::getCreated);
		listW.orderByDesc(Task::getCreated);

		List<Task> rows;
		long totalOut;
		if (q.getPage() > 0) {
			Page<Task> page = new Page<>(q.getPage(), q.getPageSize(), false);
			taskMapper.selectPage(page, listW);
			page.setTotal(total);
			rows = page.getRecords();
			totalOut = page.getTotal();
		} else {
			rows = taskMapper.selectList(listW);
			totalOut = total;
		}

		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (Task row : rows) {
			list.add(toRow(row));
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalOut);
		result.put("list", list);
		return result;
	}

	private static LambdaQueryWrapper<Task> baseWhereWrapper(long companyId, TaskListQuery q) {
		LambdaQueryWrapper<Task> w = new LambdaQueryWrapper<>();
		w.eq(Task::getCompanyId, companyId);
		if (q.isTaskNameContainsActive()) {
			String escaped = escapeLikeContains(q.getTaskNameContains());
			w.like(Task::getTaskName, "%" + escaped + "%");
		}
		if (q.isTemplateNameContainsActive()) {
			String escaped = escapeLikeContains(q.getTemplateNameContains());
			w.like(Task::getTemplateName, "%" + escaped + "%");
		}
		if (q.isStatusKeyPresent()) {
			w.eq(Task::getStatus, q.getStatusValue());
		}
		applySendAtRange(w, q.getTimeStartParts());
		return w;
	}

	private static void applySendAtRange(LambdaQueryWrapper<Task> w, List<String> timeStartParts) {
		if (timeStartParts == null || timeStartParts.isEmpty()) {
			return;
		}
		if (timeStartParts.size() == 1) {
			throw new BadRequestException("时间筛选条件不完整");
		}
		long begin = parseStrtotimeToEpoch(timeStartParts.get(0));
		long end = parseStrtotimeToEpoch(timeStartParts.get(1));
		w.ge(Task::getSendAt, (int) begin);
		w.le(Task::getSendAt, (int) end);
	}

	private static long parseStrtotimeToEpoch(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("time_start 元素不可为空");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException ignored) {
		}
		ZoneId z = ZoneId.systemDefault();
		try {
			LocalDateTime ldt = LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return ldt.atZone(z).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(t, FMT_DT);
			return ldt.atZone(z).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDate d = LocalDate.parse(t, FMT_D);
			return d.atStartOfDay(z).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDate d = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
			return d.atStartOfDay(z).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		throw new BadRequestException("time_start 时间格式无效: " + t);
	}

	private static String escapeLikeContains(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Map<String, Object> toRow(Task row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("task_name", row.getTaskName());
		m.put("template_name", row.getTemplateName());
		m.put("send_at", row.getSendAt());
		m.put("total_num", row.getTotalNum() != null ? row.getTotalNum() : 0);
		m.put("failed_num", row.getFailedNum() != null ? row.getFailedNum() : 0);
		m.put("status", row.getStatus());
		m.put("created", row.getCreated());
		return m;
	}

	public static final class TaskListQuery {
		private int page = 1;
		private int pageSize = 10;
		private boolean taskNameContainsActive;
		private String taskNameContains;
		private boolean templateNameContainsActive;
		private String templateNameContains;
		private boolean statusKeyPresent;
		private String statusValue;
		private List<String> timeStartParts;

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public int getPageSize() {
			return pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public boolean isTaskNameContainsActive() {
			return taskNameContainsActive;
		}

		public void setTaskNameContainsActive(boolean taskNameContainsActive) {
			this.taskNameContainsActive = taskNameContainsActive;
		}

		public String getTaskNameContains() {
			return taskNameContains;
		}

		public void setTaskNameContains(String taskNameContains) {
			this.taskNameContains = taskNameContains;
		}

		public boolean isTemplateNameContainsActive() {
			return templateNameContainsActive;
		}

		public void setTemplateNameContainsActive(boolean templateNameContainsActive) {
			this.templateNameContainsActive = templateNameContainsActive;
		}

		public String getTemplateNameContains() {
			return templateNameContains;
		}

		public void setTemplateNameContains(String templateNameContains) {
			this.templateNameContains = templateNameContains;
		}

		public boolean isStatusKeyPresent() {
			return statusKeyPresent;
		}

		public void setStatusKeyPresent(boolean statusKeyPresent) {
			this.statusKeyPresent = statusKeyPresent;
		}

		public String getStatusValue() {
			return statusValue;
		}

		public void setStatusValue(String statusValue) {
			this.statusValue = statusValue;
		}

		public List<String> getTimeStartParts() {
			return timeStartParts;
		}

		public void setTimeStartParts(List<String> timeStartParts) {
			this.timeStartParts = timeStartParts;
		}
	}
}
