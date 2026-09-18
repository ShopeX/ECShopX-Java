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

import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReservationAutoExpireCancelService {

	private final ReservationSettingQueryService reservationSettingQueryService;
	private final ReservationRecordMapper reservationRecordMapper;
	private final Clock clock;

	public ReservationAutoExpireCancelService(
			ReservationSettingQueryService reservationSettingQueryService,
			ReservationRecordMapper reservationRecordMapper,
			@Autowired(required = false) Clock clock) {
		this.reservationSettingQueryService = reservationSettingQueryService;
		this.reservationRecordMapper = reservationRecordMapper;
		this.clock = clock != null ? clock : Clock.systemUTC();
	}

	public void applyCancelOrNotToShopIfSuccess(long companyId, long recordId) {
		long epochSecond = clock.instant().getEpochSecond();
		ReservationSetting setting = reservationSettingQueryService.findByCompanyId(companyId).orElse(null);
		Integer cancelMinute = setting != null ? setting.getCancelMinute() : null;
		long nowtime =
				cancelMinute != null && cancelMinute > 0 ? epochSecond - 60L * cancelMinute : epochSecond;

		ReservationRecord record =
				reservationRecordMapper.selectOne(
						new LambdaQueryWrapper<ReservationRecord>()
								.eq(ReservationRecord::getCompanyId, companyId)
								.eq(ReservationRecord::getRecordId, recordId));
		if (record == null) {
			return;
		}
		if (!"success".equals(record.getStatus())) {
			return;
		}
		Integer toShop = record.getToShopTime();
		if (toShop == null) {
			return;
		}
		long toShopTime = toShop.longValue();
		String targetStatus = toShopTime > nowtime ? "cancel" : "not_to_shop";
		record.setStatus(targetStatus);
		record.setUpdated((int) epochSecond);
		reservationRecordMapper.updateById(record);
	}
}
