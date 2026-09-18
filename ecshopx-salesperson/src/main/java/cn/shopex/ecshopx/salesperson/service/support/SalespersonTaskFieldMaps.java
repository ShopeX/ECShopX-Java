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

package cn.shopex.ecshopx.salesperson.service.support;

import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 导购任务实体与列表/详情 API 字段映射（键序与键名与列表行一致）。 */
public final class SalespersonTaskFieldMaps {

	private SalespersonTaskFieldMaps() {}

	public static Map<String, Object> toDetailFields(SalespersonTask entity, ObjectMapper objectMapper) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("task_id", entity.getTaskId());
		row.put("company_id", entity.getCompanyId());
		row.put("start_time", entity.getStartTime());
		row.put("end_time", entity.getEndTime());
		row.put("task_name", entity.getTaskName());
		row.put("task_type", entity.getTaskType());
		row.put("task_quota", entity.getTaskQuota());
		row.put("pics", parsePics(entity.getPics(), objectMapper));
		row.put("task_content", entity.getTaskContent());
		row.put("use_all_distributor", entity.getUseAllDistributor());
		row.put("disabled", entity.getDisabled());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		return row;
	}

	private static Object parsePics(String pics, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(pics)) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(pics, Object.class);
		} catch (JsonProcessingException e) {
			return pics;
		}
	}
}
