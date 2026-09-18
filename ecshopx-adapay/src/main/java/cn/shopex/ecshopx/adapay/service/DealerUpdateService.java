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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerUpdateService {

	private final OperatorsCommandService operatorsCommandService;

	public DealerUpdateService(OperatorsCommandService operatorsCommandService) {
		this.operatorsCommandService = operatorsCommandService;
	}

	public void update(long companyId, String operatorIdPath, String passwordPlain) {
		if (operatorIdPath == null || operatorIdPath.trim().isEmpty()) {
			throw new BadRequestException("请选择要修改的账号");
		}
		long operatorId;
		try {
			operatorId = Long.parseLong(operatorIdPath.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("operator_id 格式不正确");
		}

		if (passwordPlain == null || passwordPlain.isBlank()) {
			throw new BadRequestException("密码不能为空");
		}
		if (passwordPlain.length() < 6 || passwordPlain.length() > 16) {
			throw new BadRequestException("密码必须6-16位");
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("password", passwordPlain);
		operatorsCommandService.updateOperator(operatorId, companyId, payload);
	}
}
