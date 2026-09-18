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

import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AliyunsmsRecordListService {

	private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FMT_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final RecordMapper recordMapper;
	private final TaskMapper taskMapper;
	private final SceneMapper sceneMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AliyunsmsRecordListService(
			RecordMapper recordMapper,
			TaskMapper taskMapper,
			SceneMapper sceneMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.recordMapper = recordMapper;
		this.taskMapper = taskMapper;
		this.sceneMapper = sceneMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getList(long companyId, RecordListQuery q) {
		Map<String, Object> result = new LinkedHashMap<>();
		List<Long> taskIdsFromName = null;
		if (isNonBlankFilter(q.getTaskName())) {
			List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
					.eq(Task::getCompanyId, companyId)
					.like(Task::getTaskName, q.getTaskName().trim()));
			taskIdsFromName = tasks.stream().map(Task::getId).filter(Objects::nonNull).collect(Collectors.toList());
			if (taskIdsFromName.isEmpty()) {
				result.put("count", 0);
				result.put("list", List.of());
				return result;
			}
		}

		LambdaQueryWrapper<Record> w = new LambdaQueryWrapper<>();
		w.eq(Record::getCompanyId, companyId);
		if (q.isTemplateTypeKeyPresent()) {
			w.eq(Record::getTemplateType, q.getTemplateTypeValue());
		}
		if (q.isStatusKeyPresent()) {
			w.eq(Record::getStatus, q.getStatusValue());
		}
		if (isNonBlankFilter(q.getTemplateCode())) {
			w.like(Record::getTemplateCode, likeContains(q.getTemplateCode().trim()));
		}
		if (isNonBlankFilter(q.getMobile())) {
			w.eq(Record::getMobile, sensitiveFieldEncryptor.encrypt(q.getMobile().trim()));
		}
		if (isNonBlankFilter(q.getSmsContent())) {
			w.like(Record::getSmsContent, likeContains(q.getSmsContent().trim()));
		}
		if (taskIdsFromName != null) {
			w.in(Record::getTaskId, toIntegerList(taskIdsFromName));
		} else if (isNonBlankFilter(q.getTaskId())) {
			List<Long> ids = parseTaskIds(q.getTaskId().trim());
			if (ids.isEmpty()) {
				result.put("total_count", 0L);
				result.put("list", List.of());
				return result;
			}
			if (ids.size() == 1) {
				w.eq(Record::getTaskId, ids.get(0).intValue());
			} else {
				w.in(Record::getTaskId, toIntegerList(ids));
			}
		}
		if (q.getTimeStart() != null && !q.getTimeStart().isEmpty()) {
			List<String> ts = q.getTimeStart();
			if (ts.size() == 1) {
				// Single bound only: not a valid closed interval for filtering; surface as embedded failure (HTTP 200 + status 500).
				throw new BadRequestException("时间筛选条件不完整");
			}
			long begin = parseStrtotimeToEpoch(ts.get(0));
			long end = parseStrtotimeToEpoch(ts.get(1));
			w.ge(Record::getCreated, (int) begin);
			w.le(Record::getCreated, (int) end);
		}
		w.orderByDesc(Record::getCreated);

		Page<Record> page = new Page<>(q.getPage(), q.getPageSize());
		Page<Record> out = recordMapper.selectPage(page, w);
		if (out.getRecords().isEmpty()) {
			result.put("total_count", out.getTotal());
			result.put("list", List.of());
			return result;
		}

		Set<Long> sceneIdSet = new LinkedHashSet<>();
		for (Record r : out.getRecords()) {
			if (r.getSceneId() != null) {
				sceneIdSet.add(r.getSceneId().longValue());
			}
		}
		Map<Long, String> sceneNames = new LinkedHashMap<>();
		if (!sceneIdSet.isEmpty()) {
			List<Scene> scenes = sceneMapper.selectList(new LambdaQueryWrapper<Scene>()
					.eq(Scene::getCompanyId, companyId)
					.in(Scene::getId, sceneIdSet));
			for (Scene s : scenes) {
				if (s.getId() != null && s.getSceneName() != null) {
					sceneNames.put(s.getId(), s.getSceneName());
				}
			}
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (Record r : out.getRecords()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", String.valueOf(r.getId()));
			row.put("company_id", r.getCompanyId());
			String plainMobile = sensitiveFieldEncryptor.decrypt(r.getMobile());
			row.put("mobile", plainMobile);
			if (r.getSceneId() != null) {
				row.put("scene_id", r.getSceneId());
			}
			row.put("template_code", r.getTemplateCode());
			row.put("template_type", r.getTemplateType());
			row.put("sms_content", r.getSmsContent());
			row.put("status", r.getStatus());
			row.put("created", r.getCreated());
			if (r.getSceneId() != null) {
				String sn = sceneNames.get(r.getSceneId().longValue());
				if (sn != null) {
					row.put("scene_name", sn);
				}
			}
			rows.add(row);
		}
		result.put("total_count", out.getTotal());
		result.put("list", rows);
		return result;
	}

	private static List<Integer> toIntegerList(List<Long> ids) {
		List<Integer> r = new ArrayList<>(ids.size());
		for (Long id : ids) {
			r.add(id.intValue());
		}
		return r;
	}

	private static List<Long> parseTaskIds(String raw) {
		List<Long> out = new ArrayList<>();
		for (String p : raw.split(",")) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private static boolean isNonBlankFilter(String v) {
		if (!StringUtils.hasText(v)) {
			return false;
		}
		String t = v.trim();
		return !"0".equals(t);
	}

	private static String likeContains(String v) {
		String e = v.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + e + "%";
	}

	private long parseStrtotimeToEpoch(String raw) {
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

	public static final class RecordListQuery {
		private int page = 1;
		private int pageSize = 10;
		private boolean statusKeyPresent;
		private String statusValue;
		private boolean templateTypeKeyPresent;
		private String templateTypeValue;
		private String templateCode;
		private String mobile;
		private String smsContent;
		private String taskName;
		private String taskId;
		private List<String> timeStart;

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

		public boolean isTemplateTypeKeyPresent() {
			return templateTypeKeyPresent;
		}

		public void setTemplateTypeKeyPresent(boolean templateTypeKeyPresent) {
			this.templateTypeKeyPresent = templateTypeKeyPresent;
		}

		public String getTemplateTypeValue() {
			return templateTypeValue;
		}

		public void setTemplateTypeValue(String templateTypeValue) {
			this.templateTypeValue = templateTypeValue;
		}

		public String getTemplateCode() {
			return templateCode;
		}

		public void setTemplateCode(String templateCode) {
			this.templateCode = templateCode;
		}

		public String getMobile() {
			return mobile;
		}

		public void setMobile(String mobile) {
			this.mobile = mobile;
		}

		public String getSmsContent() {
			return smsContent;
		}

		public void setSmsContent(String smsContent) {
			this.smsContent = smsContent;
		}

		public String getTaskName() {
			return taskName;
		}

		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}

		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}

		public List<String> getTimeStart() {
			return timeStart;
		}

		public void setTimeStart(List<String> timeStart) {
			this.timeStart = timeStart;
		}
	}
}
