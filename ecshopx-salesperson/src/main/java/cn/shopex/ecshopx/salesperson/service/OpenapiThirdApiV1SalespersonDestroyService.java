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
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1SalespersonDestroyService {

	private static final String MSG_QUERY_ERROR = "导购员信息查询错误";

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final AdminSalespersonDeleteService adminSalespersonDeleteService;

	public OpenapiThirdApiV1SalespersonDestroyService(
			ShopSalespersonMapper shopSalespersonMapper,
			AdminSalespersonDeleteService adminSalespersonDeleteService) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.adminSalespersonDeleteService = adminSalespersonDeleteService;
	}

	public void executeDestroySalesperson(long companyId, String employeeNumber) {
		ShopSalesperson row = shopSalespersonMapper.selectOne(
				new LambdaQueryWrapper<ShopSalesperson>()
						.eq(ShopSalesperson::getCompanyId, companyId)
						.eq(ShopSalesperson::getWorkUserid, employeeNumber)
						.last("LIMIT 1")
						.select(ShopSalesperson::getSalespersonId));

		if (row == null) {
			throw new ResourceException(MSG_QUERY_ERROR);
		}

		adminSalespersonDeleteService.deleteSalesperson(companyId, row.getSalespersonId());
	}
}
