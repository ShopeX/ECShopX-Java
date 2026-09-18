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

import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSalespersonDeleteService {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;

	public AdminSalespersonDeleteService(ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteSalesperson(long companyId, long salespersonId) {
		shopSalespersonMapper.delete(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getCompanyId, companyId));
		shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
				.eq(ShopsRelSalesperson::getSalespersonId, salespersonId)
				.eq(ShopsRelSalesperson::getCompanyId, companyId));
	}
}
