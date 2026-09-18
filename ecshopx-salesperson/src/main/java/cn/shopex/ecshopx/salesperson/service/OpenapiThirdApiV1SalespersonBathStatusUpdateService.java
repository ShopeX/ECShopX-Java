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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenapiThirdApiV1SalespersonBathStatusUpdateService {

	private static final String MSG_QUERY_ERROR = "导购员信息查询错误";

	private final ShopSalespersonMapper shopSalespersonMapper;

	public OpenapiThirdApiV1SalespersonBathStatusUpdateService(ShopSalespersonMapper shopSalespersonMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
	}

	public void executeBathUpdateSalespersonStatus(
			long companyId, String employeeNumber, String salespersonStatus) {
		List<String> workUserids = splitEmployeeNumber(employeeNumber);
		log.info("bathUpdateSalespersonStatus filter===> companyId={}, workUserids={}", companyId, workUserids);

		List<ShopSalesperson> rows = shopSalespersonMapper.selectList(
				new LambdaQueryWrapper<ShopSalesperson>()
						.eq(ShopSalesperson::getCompanyId, companyId)
						.in(ShopSalesperson::getWorkUserid, workUserids)
						.select(ShopSalesperson::getSalespersonId));

		if (rows == null || rows.isEmpty()) {
			throw new ResourceException(MSG_QUERY_ERROR);
		}

		List<Long> salespersonIds = rows.stream().map(ShopSalesperson::getSalespersonId).toList();
		log.info("bathUpdateSalespersonStatus salespersonList===> ids={}", salespersonIds);

		String isValid = resolveIsValid(salespersonStatus);
		log.info(
				"bathUpdateSalespersonStatus update filter===> companyId={}, salespersonIds={}",
				companyId,
				salespersonIds);
		log.info("bathUpdateSalespersonStatus updateData===> is_valid={}", isValid);

		int affected = shopSalespersonMapper.update(
				null,
				new LambdaUpdateWrapper<ShopSalesperson>()
						.set(ShopSalesperson::getIsValid, isValid)
						.eq(ShopSalesperson::getCompanyId, companyId)
						.in(ShopSalesperson::getSalespersonId, salespersonIds));
		log.info("bathUpdateSalespersonStatus result===> affected={}", affected);
	}

	private static List<String> splitEmployeeNumber(String employeeNumber) {
		if (employeeNumber == null) {
			return List.of("");
		}
		return List.of(employeeNumber.split(",", -1));
	}

	private static String resolveIsValid(String salespersonStatus) {
		return "1".equals(salespersonStatus) ? "true" : "false";
	}
}
