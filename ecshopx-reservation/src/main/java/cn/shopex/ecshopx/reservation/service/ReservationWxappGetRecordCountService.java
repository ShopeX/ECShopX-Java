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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReservationWxappGetRecordCountService {

	private static final List<String> GET_COUNT_STATUSES = List.of("success", "to_the_shop");

	private final ReservationRecordMapper reservationRecordMapper;

	public ReservationWxappGetRecordCountService(ReservationRecordMapper reservationRecordMapper) {
		this.reservationRecordMapper = reservationRecordMapper;
	}

	public long count(Map<String, Object> auth, String rightsIdParam) {
		long companyId = toLong(auth.get("company_id"));
		Long userId = resolveQueryUserId(auth.get("user_id"));
		if (userId == null) {
			throw new UnauthorizedException("未登录");
		}

		LambdaQueryWrapper<ReservationRecord> w = Wrappers.lambdaQuery();
		w.eq(ReservationRecord::getCompanyId, companyId);
		w.eq(ReservationRecord::getUserId, userId);
		w.in(ReservationRecord::getStatus, GET_COUNT_STATUSES);

		if (rightsIdParam == null || rightsIdParam.trim().isEmpty()) {
			w.isNull(ReservationRecord::getRightsId);
		} else {
			String trimmed = rightsIdParam.trim();
			long rightsId;
			try {
				rightsId = Long.parseLong(trimmed);
			} catch (NumberFormatException e) {
				throw new BadRequestException("rights_id 参数格式错误");
			}
			w.eq(ReservationRecord::getRightsId, rightsId);
		}

		Long cnt = reservationRecordMapper.selectCount(w);
		return cnt == null ? 0L : cnt;
	}

	static Long resolveQueryUserId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String us = raw.toString().trim();
		if (us.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(us);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
