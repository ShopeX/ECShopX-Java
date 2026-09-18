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

import cn.shopex.ecshopx.salesperson.domain.SalespersonRelCoupon;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonRelCouponMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalespersonCouponCreateService {

	private final SalespersonCouponGrantSettingRedisService grantSettingRedisService;
	private final SalespersonRelCouponMapper mapper;

	public SalespersonCouponCreateService(
			SalespersonCouponGrantSettingRedisService grantSettingRedisService,
			SalespersonRelCouponMapper mapper) {
		this.grantSettingRedisService = grantSettingRedisService;
		this.mapper = mapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void create(
			long companyId,
			String limitCycle,
			String grantPerUserTotal,
			String grantTotal,
			List<CouponItem> coupons) {
		grantSettingRedisService.writeGrantLimitFields(companyId, limitCycle, grantPerUserTotal, grantTotal);
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (CouponItem item : coupons) {
			mapper.delete(new LambdaQueryWrapper<SalespersonRelCoupon>()
					.eq(SalespersonRelCoupon::getCompanyId, companyId)
					.eq(SalespersonRelCoupon::getCouponId, item.couponId()));
			SalespersonRelCoupon row = new SalespersonRelCoupon();
			row.setCompanyId(companyId);
			row.setCouponId(item.couponId());
			row.setSendNum(item.sendNum());
			row.setCreated(now);
			row.setUpdated(now);
			mapper.insert(row);
		}
	}

	public record CouponItem(long couponId, long sendNum) {}
}
