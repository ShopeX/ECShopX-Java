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
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class WorkShiftDeleteService {

	private final WorkShiftMapper workShiftMapper;

	public WorkShiftDeleteService(WorkShiftMapper workShiftMapper) {
		this.workShiftMapper = workShiftMapper;
	}

	public boolean deleteWorkShift(long companyId, Long workShiftIdOrNull) {
		LambdaQueryWrapper<WorkShift> queryWrapper =
				new LambdaQueryWrapper<WorkShift>().eq(WorkShift::getCompanyId, companyId);
		if (workShiftIdOrNull != null) {
			queryWrapper = queryWrapper.eq(WorkShift::getId, workShiftIdOrNull);
		} else {
			queryWrapper = queryWrapper.isNull(WorkShift::getId);
		}

		WorkShift row = workShiftMapper.selectOne(queryWrapper);
		if (row == null) {
			return false;
		}

		long nowSec = Instant.now().getEpochSecond();
		Long workDate = row.getWorkDate();
		if (workDate == null || workDate <= nowSec) {
			throw new ResourceException("历史排班不可删除");
		}

		LambdaQueryWrapper<WorkShift> deleteWrapper =
				new LambdaQueryWrapper<WorkShift>()
						.eq(WorkShift::getCompanyId, companyId)
						.eq(WorkShift::getId, row.getId());
		int affected = workShiftMapper.delete(deleteWrapper);
		return affected == 1;
	}
}
