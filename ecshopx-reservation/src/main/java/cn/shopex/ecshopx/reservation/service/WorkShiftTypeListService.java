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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkShiftTypeListService {

	private static final long PAGE_CURRENT = 1L;
	private static final long PAGE_SIZE = 10L;

	private final WorkShiftTypeMapper workShiftTypeMapper;

	public WorkShiftTypeListService(WorkShiftTypeMapper workShiftTypeMapper) {
		this.workShiftTypeMapper = workShiftTypeMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> listValidShiftTypes(long companyId) {
		LambdaQueryWrapper<WorkShiftType> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WorkShiftType::getCompanyId, companyId);
		wrapper.eq(WorkShiftType::getStatus, "valid");
		wrapper.orderByDesc(WorkShiftType::getTypeId);

		Page<WorkShiftType> page = new Page<>(PAGE_CURRENT, PAGE_SIZE);
		workShiftTypeMapper.selectPage(page, wrapper);

		LinkedHashMap<String, Object> rest = new LinkedHashMap<>();
		rest.put("typeName", "休息");
		rest.put("beginTime", "00:00");
		rest.put("endTime", "23:59");
		rest.put("typeId", "-1");

		List<LinkedHashMap<String, Object>> mergedList = new ArrayList<>();
		mergedList.add(rest);
		for (WorkShiftType row : page.getRecords()) {
			mergedList.add(toListRowMap(row));
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("list", mergedList);
		result.put("total_count", mergedList.size());
		return result;
	}

	private static LinkedHashMap<String, Object> toListRowMap(WorkShiftType row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("typeId", row.getTypeId());
		m.put("companyId", row.getCompanyId());
		m.put("typeName", row.getTypeName());
		m.put("beginTime", row.getBeginTime());
		m.put("endTime", row.getEndTime());
		m.put("status", row.getStatus());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}
}
