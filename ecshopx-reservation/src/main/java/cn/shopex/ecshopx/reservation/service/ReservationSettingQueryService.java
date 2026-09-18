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

import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationSettingMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReservationSettingQueryService {

	private final ReservationSettingMapper reservationSettingMapper;

	public ReservationSettingQueryService(ReservationSettingMapper reservationSettingMapper) {
		this.reservationSettingMapper = reservationSettingMapper;
	}

	public Optional<ReservationSetting> findByCompanyId(long companyId) {
		ReservationSetting row = reservationSettingMapper.selectOne(
				Wrappers.<ReservationSetting>lambdaQuery().eq(ReservationSetting::getCompanyId, companyId));
		return Optional.ofNullable(row);
	}
}
