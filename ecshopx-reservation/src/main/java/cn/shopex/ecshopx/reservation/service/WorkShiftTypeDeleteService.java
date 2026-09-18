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
import cn.shopex.ecshopx.reservation.domain.WorkShift;
import cn.shopex.ecshopx.reservation.domain.WorkShiftType;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class, readOnly = false)
public class WorkShiftTypeDeleteService {

	private final WorkShiftMapper workShiftMapper;
	private final WorkShiftTypeMapper workShiftTypeMapper;

	public WorkShiftTypeDeleteService(
			WorkShiftMapper workShiftMapper, WorkShiftTypeMapper workShiftTypeMapper) {
		this.workShiftMapper = workShiftMapper;
		this.workShiftTypeMapper = workShiftTypeMapper;
	}

	public boolean deleteShiftType(long companyId, String typeId) {
		long now = Instant.now().getEpochSecond();

		LambdaQueryWrapper<WorkShift> futureShifts =
				new LambdaQueryWrapper<WorkShift>()
						.eq(WorkShift::getCompanyId, companyId)
						.eq(WorkShift::getShiftTypeId, typeId)
						.ge(WorkShift::getWorkDate, now);

		if (workShiftMapper.selectCount(futureShifts) > 0) {
			WorkShiftType typeRow =
					workShiftTypeMapper.selectOne(
							new LambdaQueryWrapper<WorkShiftType>()
									.eq(WorkShiftType::getTypeId, typeId)
									.eq(WorkShiftType::getCompanyId, companyId));
			if (typeRow == null) {
				return false;
			}
			workShiftMapper.delete(futureShifts);
			workShiftTypeMapper.deleteById(typeRow.getTypeId());
			return true;
		}

		LambdaQueryWrapper<WorkShift> pastOrPresent =
				new LambdaQueryWrapper<WorkShift>()
						.eq(WorkShift::getCompanyId, companyId)
						.eq(WorkShift::getShiftTypeId, typeId)
						.le(WorkShift::getWorkDate, now);

		String status = workShiftMapper.selectCount(pastOrPresent) > 0 ? "invalid" : "delete";
		return deleteData(companyId, typeId, status);
	}

	private boolean deleteData(long companyId, String typeId, String status) {
		WorkShiftType row =
				workShiftTypeMapper.selectOne(
						new LambdaQueryWrapper<WorkShiftType>()
								.eq(WorkShiftType::getTypeId, typeId)
								.eq(WorkShiftType::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException("排班类型删除失败");
		}
		if ("invalid".equals(status)) {
			int ts = (int) Instant.now().getEpochSecond();
			row.setStatus("invalid");
			row.setUpdated(ts);
			workShiftTypeMapper.updateById(row);
			return true;
		}
		if ("delete".equals(status)) {
			workShiftTypeMapper.deleteById(row.getTypeId());
			return true;
		}
		throw new IllegalStateException("unexpected status: " + status);
	}
}
