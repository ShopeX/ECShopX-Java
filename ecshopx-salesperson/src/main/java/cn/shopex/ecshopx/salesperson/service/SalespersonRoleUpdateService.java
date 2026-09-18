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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SalespersonRoleUpdateService {

	private final ShopSalespersonMapper shopSalespersonMapper;

	public SalespersonRoleUpdateService(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateSalesmanRole(long companyId, String salesmanId, String role) {
		String trimmed = salesmanId == null ? "" : salesmanId.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new ResourceException("更新的人员不存在");
		}
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("更新的人员不存在");
		}

		ShopSalesperson existing = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, id)
				.eq(ShopSalesperson::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (existing == null) {
			throw new ResourceException("更新的人员不存在");
		}

		LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, id)
				.eq(ShopSalesperson::getCompanyId, companyId)
				.set(ShopSalesperson::getRole, role);
		int rows = shopSalespersonMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
