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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRecord;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRecordMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.service.support.SalespersonTaskFieldMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SalespersonTaskListService {

	private static final String TASK_DISABLED = "DISABLED";

	private final SalespersonTaskMapper salespersonTaskMapper;
	private final SalespersonTaskRecordMapper salespersonTaskRecordMapper;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ObjectMapper objectMapper;

	public SalespersonTaskListService(SalespersonTaskMapper salespersonTaskMapper,
			SalespersonTaskRecordMapper salespersonTaskRecordMapper,
			ShopSalespersonMapper shopSalespersonMapper,
			ObjectMapper objectMapper) {
		this.salespersonTaskMapper = salespersonTaskMapper;
		this.salespersonTaskRecordMapper = salespersonTaskRecordMapper;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> lists(long companyId, String status, int page, int pageSize) {
		long now = java.time.Instant.now().getEpochSecond();

		LambdaQueryWrapper<SalespersonTask> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SalespersonTask::getCompanyId, companyId);
		applyStatusFilter(wrapper, status, now);
		wrapper.orderByDesc(SalespersonTask::getCreated);

		Page<SalespersonTask> mpPage = new Page<>(page, pageSize);
		Page<SalespersonTask> result = salespersonTaskMapper.selectPage(mpPage, wrapper);

		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (SalespersonTask entity : result.getRecords()) {
			String derivedStatus = deriveStatus(entity, now);
			rowMaps.add(toRowMap(entity, derivedStatus));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", result.getTotal());
		out.put("list", rowMaps);
		return out;
	}

	public Map<String, Object> statistics(long companyId, Long taskId, int page, int pageSize) {
		if (taskId == null) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", new ArrayList<>());
			empty.put("task", Collections.emptyList());
			return empty;
		}

		LambdaQueryWrapper<SalespersonTaskRecord> w = new LambdaQueryWrapper<>();
		w.eq(SalespersonTaskRecord::getCompanyId, companyId)
				.eq(SalespersonTaskRecord::getTaskId, taskId)
				.orderByDesc(SalespersonTaskRecord::getTimes);
		Page<SalespersonTaskRecord> mpPage = new Page<>(page, pageSize);
		Page<SalespersonTaskRecord> pageResult = salespersonTaskRecordMapper.selectPage(mpPage, w);
		long totalCount = pageResult.getTotal();
		List<SalespersonTaskRecord> records = pageResult.getRecords();

		LambdaQueryWrapper<SalespersonTask> tw = new LambdaQueryWrapper<>();
		tw.eq(SalespersonTask::getCompanyId, companyId).eq(SalespersonTask::getTaskId, taskId);
		SalespersonTask taskEntity = salespersonTaskMapper.selectOne(tw);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		List<Map<String, Object>> list = new ArrayList<>();
		out.put("list", list);
		out.put("task", taskEntity == null ? Collections.emptyList() : taskToStatisticsTaskMap(taskEntity));

		if (totalCount == 0) {
			return out;
		}

		List<Long> idList = records.stream()
				.map(SalespersonTaskRecord::getSalespersonId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		Map<Long, String> idToName = new LinkedHashMap<>();
		if (!idList.isEmpty()) {
			LambdaQueryWrapper<ShopSalesperson> sw = new LambdaQueryWrapper<>();
			sw.eq(ShopSalesperson::getCompanyId, companyId).in(ShopSalesperson::getSalespersonId, idList);
			List<ShopSalesperson> sps = shopSalespersonMapper.selectList(sw);
			for (ShopSalesperson sp : sps) {
				if (sp.getSalespersonId() != null) {
					idToName.put(sp.getSalespersonId(), sp.getName() != null ? sp.getName() : "");
				}
			}
		}

		Integer quota = taskEntity == null ? null : taskEntity.getTaskQuota();

		for (SalespersonTaskRecord record : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", record.getId());
			row.put("company_id", record.getCompanyId());
			row.put("task_id", record.getTaskId());
			row.put("distributor_id", record.getDistributorId());
			row.put("salesperson_id", record.getSalespersonId());
			row.put("times", record.getTimes());
			row.put("task_quota", quota);

			String name = idToName.get(record.getSalespersonId());
			row.put("salesperson_name", name != null ? name : "");

			int timesVal = record.getTimes() == null ? 0 : record.getTimes();
			if (taskEntity == null || quota == null || quota == 0) {
				row.put("percentage", "0%");
			} else {
				long pct = (long) Math.ceil((double) timesVal / (double) quota * 100.0);
				row.put("percentage", pct + "%");
			}

			list.add(row);
		}

		return out;
	}

	private void applyStatusFilter(LambdaQueryWrapper<SalespersonTask> wrapper, String status, long now) {
		String s = status == null ? "" : status.trim();
		if (s.isEmpty() || "all".equals(s)) {
			return;
		}
		switch (s) {
			case "waiting" -> wrapper.gt(SalespersonTask::getStartTime, now);
			case "ongoing" -> wrapper.lt(SalespersonTask::getStartTime, now).gt(SalespersonTask::getEndTime, now);
			case "end" -> wrapper.lt(SalespersonTask::getEndTime, now);
			case "close" -> wrapper.eq(SalespersonTask::getDisabled, TASK_DISABLED);
			default -> {
				/* no extra conditions */
			}
		}
	}

	private static String deriveStatus(SalespersonTask entity, long now) {
		if (Objects.equals(entity.getDisabled(), TASK_DISABLED)) {
			return "close";
		}
		if (entity.getStartTime() != null && entity.getStartTime() > now) {
			return "waiting";
		}
		if (entity.getEndTime() != null && entity.getEndTime() < now) {
			return "end";
		}
		return "ongoing";
	}

	private Map<String, Object> toRowMap(SalespersonTask entity, String derivedStatus) {
		Map<String, Object> row = new LinkedHashMap<>(SalespersonTaskFieldMaps.toDetailFields(entity, objectMapper));
		row.put("status", derivedStatus);
		return row;
	}

	private Map<String, Object> taskToStatisticsTaskMap(SalespersonTask entity) {
		return SalespersonTaskFieldMaps.toDetailFields(entity, objectMapper);
	}
}
