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
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ResourceLevel;
import cn.shopex.ecshopx.reservation.domain.ResourceLevelRelService;
import cn.shopex.ecshopx.reservation.domain.WorkShift;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelRelServiceMapper;
import cn.shopex.ecshopx.reservation.mapper.WorkShiftMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelDeleteService {

	private final WorkShiftMapper workShiftMapper;
	private final ReservationRecordMapper reservationRecordMapper;
	private final ResourceLevelMapper resourceLevelMapper;
	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;

	public ResourceLevelDeleteService(
			WorkShiftMapper workShiftMapper,
			ReservationRecordMapper reservationRecordMapper,
			ResourceLevelMapper resourceLevelMapper,
			ResourceLevelRelServiceMapper resourceLevelRelServiceMapper) {
		this.workShiftMapper = workShiftMapper;
		this.reservationRecordMapper = reservationRecordMapper;
		this.resourceLevelMapper = resourceLevelMapper;
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean deleteResourceLevel(long companyId, String resourceLevelIdRaw, String shopIdRaw) {
		Long parsedId = parseResourceLevelIdForDelete(resourceLevelIdRaw);
		if (parsedId == null) {
			return false;
		}

		String companyIdStr = String.valueOf(companyId);

		LambdaQueryWrapper<WorkShift> shiftW = new LambdaQueryWrapper<>();
		shiftW.eq(WorkShift::getCompanyId, companyId).eq(WorkShift::getResourceLevelId, parsedId);
		appendShopIdWorkShift(shiftW, shopIdRaw);
		if (workShiftMapper.selectCount(shiftW) > 0) {
			throw new ResourceException("该资源位已有排班，不可删除");
		}

		LambdaQueryWrapper<ReservationRecord> recW = new LambdaQueryWrapper<>();
		recW.eq(ReservationRecord::getCompanyId, companyId).eq(ReservationRecord::getResourceLevelId, parsedId);
		appendShopIdReservationRecord(recW, shopIdRaw);
		if (reservationRecordMapper.selectCount(recW) > 0) {
			throw new ResourceException("该资源位已有预约记录，不可删除");
		}

		LambdaQueryWrapper<ResourceLevel> exist = new LambdaQueryWrapper<>();
		exist.eq(ResourceLevel::getResourceLevelId, parsedId).eq(ResourceLevel::getCompanyId, companyIdStr);
		ResourceLevel row = resourceLevelMapper.selectOne(exist);
		if (row == null) {
			return false;
		}

		LambdaQueryWrapper<ResourceLevel> delMain = new LambdaQueryWrapper<>();
		delMain.eq(ResourceLevel::getResourceLevelId, parsedId).eq(ResourceLevel::getCompanyId, companyIdStr);
		appendShopIdResourceLevel(delMain, shopIdRaw);
		resourceLevelMapper.delete(delMain);

		LambdaQueryWrapper<ResourceLevelRelService> delRel = new LambdaQueryWrapper<>();
		delRel.eq(ResourceLevelRelService::getCompanyId, companyId)
				.eq(ResourceLevelRelService::getResourceLevelId, parsedId);
		resourceLevelRelServiceMapper.delete(delRel);

		return true;
	}

	/**
	 * @return null when trim-empty or non-numeric (no valid id).
	 */
	private static Long parseResourceLevelIdForDelete(String resourceLevelIdRaw) {
		if (resourceLevelIdRaw == null) {
			return null;
		}
		String t = resourceLevelIdRaw.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void appendShopIdWorkShift(LambdaQueryWrapper<WorkShift> w, String shopIdRaw) {
		if (shopIdRaw == null) {
			w.isNull(WorkShift::getShopId);
		} else if (shopIdRaw.trim().isEmpty()) {
			w.eq(WorkShift::getShopId, 0L);
		} else {
			String t = shopIdRaw.trim();
			try {
				w.eq(WorkShift::getShopId, Long.parseLong(t));
			} catch (NumberFormatException e) {
				w.apply("shop_id = {0}", t);
			}
		}
	}

	private static void appendShopIdReservationRecord(LambdaQueryWrapper<ReservationRecord> w, String shopIdRaw) {
		if (shopIdRaw == null) {
			w.isNull(ReservationRecord::getShopId);
		} else if (shopIdRaw.trim().isEmpty()) {
			w.eq(ReservationRecord::getShopId, 0L);
		} else {
			String t = shopIdRaw.trim();
			try {
				w.eq(ReservationRecord::getShopId, Long.parseLong(t));
			} catch (NumberFormatException e) {
				w.apply("shop_id = {0}", t);
			}
		}
	}

	private static void appendShopIdResourceLevel(LambdaQueryWrapper<ResourceLevel> w, String shopIdRaw) {
		if (shopIdRaw == null) {
			w.isNull(ResourceLevel::getShopId);
		} else if (shopIdRaw.trim().isEmpty()) {
			w.eq(ResourceLevel::getShopId, "");
		} else {
			w.eq(ResourceLevel::getShopId, shopIdRaw.trim());
		}
	}
}
