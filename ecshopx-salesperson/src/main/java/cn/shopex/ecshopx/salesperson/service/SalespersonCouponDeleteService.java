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
import org.springframework.stereotype.Service;

@Service
public class SalespersonCouponDeleteService {

	private final SalespersonRelCouponMapper mapper;

	public SalespersonCouponDeleteService(SalespersonRelCouponMapper mapper) {
		this.mapper = mapper;
	}

	public void delete(long companyId, long relCouponId) {
		mapper.delete(new LambdaQueryWrapper<SalespersonRelCoupon>()
				.eq(SalespersonRelCoupon::getId, relCouponId)
				.eq(SalespersonRelCoupon::getCompanyId, companyId));
	}
}
