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

package cn.shopex.ecshopx.kaquan.service;

import cn.shopex.ecshopx.common.cron.SalespersonGiveCouponsCountPort;
import cn.shopex.ecshopx.kaquan.domain.SalespersonGiveCoupons;
import cn.shopex.ecshopx.kaquan.mapper.SalespersonGiveCouponsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalespersonGiveCouponsCountPortImpl implements SalespersonGiveCouponsCountPort {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final SalespersonGiveCouponsMapper salespersonGiveCouponsMapper;

	@Override
	public int countSuccessRowsForDate(long companyId, long salespersonId, int dateYmd) {
		LocalDate d = LocalDate.parse(Integer.toString(dateYmd), YMD);
		ZonedDateTime startZ = d.atStartOfDay(APP_ZONE);
		long startSec = startZ.toEpochSecond();
		long endSecExcl = startZ.plusDays(1).toEpochSecond();
		int start = (int) startSec;
		int endExcl = (int) endSecExcl;
		long n = salespersonGiveCouponsMapper.selectCount(
				new LambdaQueryWrapper<SalespersonGiveCoupons>()
						.eq(SalespersonGiveCoupons::getCompanyId, companyId)
						.eq(SalespersonGiveCoupons::getSalespersonId, salespersonId)
						.eq(SalespersonGiveCoupons::getStatus, 1)
						.ge(SalespersonGiveCoupons::getGiveTime, start)
						.lt(SalespersonGiveCoupons::getGiveTime, endExcl));
		if (n > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) n;
	}
}
