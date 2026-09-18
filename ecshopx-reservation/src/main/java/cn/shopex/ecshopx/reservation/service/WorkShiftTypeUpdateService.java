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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class WorkShiftTypeUpdateService {

	private final WorkShiftTypeMapper workShiftTypeMapper;

	public WorkShiftTypeUpdateService(WorkShiftTypeMapper workShiftTypeMapper) {
		this.workShiftTypeMapper = workShiftTypeMapper;
	}

	public Map<String, Object> updateShiftTypeName(long companyId, long typeId, String typeName) {
		WorkShiftType row =
				workShiftTypeMapper.selectOne(
						new LambdaQueryWrapper<WorkShiftType>()
								.eq(WorkShiftType::getTypeId, typeId)
								.eq(WorkShiftType::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException("排班类型不存在");
		}
		int now = (int) Instant.now().getEpochSecond();
		row.setTypeName(typeName);
		row.setUpdated(now);
		workShiftTypeMapper.updateById(row);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("type_id", row.getTypeId());
		out.put("type_name", typeName);
		out.put("company_id", companyId);
		out.put("begin_time", row.getBeginTime());
		out.put("end_time", row.getEndTime());
		return out;
	}
}
