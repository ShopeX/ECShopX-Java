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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerDelSubService {

	private final OperatorsQueryService operatorsQueryService;
	private final OperatorsCommandService operatorsCommandService;

	public DealerDelSubService(
			OperatorsQueryService operatorsQueryService,
			OperatorsCommandService operatorsCommandService) {
		this.operatorsQueryService = operatorsQueryService;
		this.operatorsCommandService = operatorsCommandService;
	}

	public void delDealerSub(String operatorId) {
		if (operatorId == null || operatorId.trim().isEmpty()) {
			throw new BadRequestException("operator_id 不能为空");
		}
		String operatorIdRaw = operatorId.trim();

		Map<String, Object> filter = Map.of("operator_id", operatorIdRaw);
		Map<String, Object> info = operatorsQueryService.getInfo(filter);
		if (info == null || info.isEmpty()) {
			throw new ResourceException("未找到删除账号");
		}
		if (isDealerMainTruthy(info.get("is_dealer_main"))) {
			throw new ResourceException("主账号不可删除");
		}

		long idForDelete = requireOperatorIdForDelete(info, operatorIdRaw);
		operatorsCommandService.deleteByOperatorIdOnly(idForDelete);
	}

	private static long requireOperatorIdForDelete(Map<String, Object> info, String operatorIdRaw) {
		Object v = info.get("operator_id");
		if (v instanceof Number n) {
			long id = n.longValue();
			if (id > 0L) {
				return id;
			}
		} else if (v instanceof String s) {
			try {
				long id = Long.parseLong(s.trim());
				if (id > 0L) {
					return id;
				}
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		try {
			long id = Long.parseLong(operatorIdRaw);
			if (id > 0L) {
				return id;
			}
		} catch (NumberFormatException e) {
			throw new ResourceException("未找到删除账号");
		}
		throw new ResourceException("未找到删除账号");
	}

	private static boolean isDealerMainTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return !"0".equals(String.valueOf(v).trim());
	}
}
