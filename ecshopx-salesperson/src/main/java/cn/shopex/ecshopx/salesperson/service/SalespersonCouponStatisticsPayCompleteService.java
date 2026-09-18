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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.SalespersonCouponStatistics;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonCouponStatisticsMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCouponStatisticsPayCompleteService {

	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SalespersonCouponStatisticsMapper salespersonCouponStatisticsMapper;

	public SalespersonCouponStatisticsPayCompleteService(
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SalespersonCouponStatisticsMapper salespersonCouponStatisticsMapper) {
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.salespersonCouponStatisticsMapper = salespersonCouponStatisticsMapper;
	}

	public void tryPayIncrement(long companyId, long salespersonId, long couponId) {
		if (salespersonId <= 0L) {
			return;
		}
		ShopsRelSalesperson shopRel = shopsRelSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopsRelSalesperson>()
				.eq(ShopsRelSalesperson::getSalespersonId, salespersonId)
				.eq(ShopsRelSalesperson::getStoreType, "distributor")
				.last("LIMIT 1"));
		if (shopRel == null || shopRel.getShopId() == null || shopRel.getShopId() <= 0L) {
			return;
		}
		long distributorId = shopRel.getShopId();
		long dateKey = Long.parseLong(LocalDate.now(ZoneId.systemDefault()).format(YMD));
		SalespersonCouponStatistics existing = salespersonCouponStatisticsMapper.selectOne(
				new LambdaQueryWrapper<SalespersonCouponStatistics>()
						.eq(SalespersonCouponStatistics::getCompanyId, companyId)
						.eq(SalespersonCouponStatistics::getSalespersonId, salespersonId)
						.eq(SalespersonCouponStatistics::getDistributorId, distributorId)
						.eq(SalespersonCouponStatistics::getCouponId, couponId)
						.eq(SalespersonCouponStatistics::getDate, dateKey)
						.last("LIMIT 1"));
		if (existing == null) {
			SalespersonCouponStatistics row = new SalespersonCouponStatistics();
			row.setCompanyId(companyId);
			row.setDistributorId(distributorId);
			row.setSalespersonId(salespersonId);
			row.setCouponId(couponId);
			row.setDate(dateKey);
			row.setSendNum(0L);
			row.setPayNum(1L);
			row.setReceiveNum(0L);
			row.setRegNum(0L);
			salespersonCouponStatisticsMapper.insert(row);
		} else {
			salespersonCouponStatisticsMapper.update(
					null,
					new LambdaUpdateWrapper<SalespersonCouponStatistics>()
							.eq(SalespersonCouponStatistics::getCompanyId, companyId)
							.eq(SalespersonCouponStatistics::getSalespersonId, salespersonId)
							.eq(SalespersonCouponStatistics::getDistributorId, distributorId)
							.eq(SalespersonCouponStatistics::getCouponId, couponId)
							.eq(SalespersonCouponStatistics::getDate, dateKey)
							.setSql("pay_num = IFNULL(pay_num,0) + 1"));
		}
	}
}
