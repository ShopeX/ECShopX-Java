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

import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerOpenOrDisableService {

	private final OperatorsMapper operatorsMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorUpdateService distributorUpdateService;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;
	private final ObjectMapper objectMapper;
	private final DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher;

	public DealerOpenOrDisableService(
			OperatorsMapper operatorsMapper,
			OperatorsQueryService operatorsQueryService,
			DistributorUpdateService distributorUpdateService,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort,
			ObjectMapper objectMapper,
			DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher) {
		this.operatorsMapper = operatorsMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorUpdateService = distributorUpdateService;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
		this.objectMapper = objectMapper;
		this.distributorUpdateEventDispatchPublisher = distributorUpdateEventDispatchPublisher;
	}

	public void openOrDisable(
			long companyId, long jwtOperatorId, String operatorIdQuery, String isDisableQuery) {
		if (operatorIdQuery == null || operatorIdQuery.trim().isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		long targetOperatorId;
		try {
			targetOperatorId = Long.parseLong(operatorIdQuery.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("operator_id 格式不正确");
		}

		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getCompanyId, companyId).eq(Operators::getOperatorId, targetOperatorId);
		Operators existing = operatorsMapper.selectOne(w);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}

		existing.setIsDisable(isDisableTruthy(isDisableQuery));
		existing.setUpdated((int) (System.currentTimeMillis() / 1000L));
		int updatedRows = operatorsMapper.updateById(existing);
		if (updatedRows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		if (isDisableTruthy(isDisableQuery)) {
			merged.put("is_valid", "false");
		} else {
			merged.put("is_valid", "true");
		}

		List<Map<String, Object>> list = parseDistributorIdsJson(existing.getDistributorIds());
		if (list != null && !list.isEmpty()) {
			for (Map<String, Object> item : list) {
				Object idObj = item.get("distributor_id");
				long distributorId;
				if (idObj instanceof Number n) {
					distributorId = n.longValue();
				} else {
					try {
						distributorId = Long.parseLong(String.valueOf(idObj).trim());
					} catch (NumberFormatException e) {
						throw new BadRequestException("distributor_id 格式不正确");
					}
				}
				merged.put("distributor_id", distributorId);
				Map<String, Object> row =
						distributorUpdateService.performUpdateAndEvents(merged, distributorId, null);
				distributorUpdateEventDispatchPublisher.publish(row);
			}
		}

		Map<String, Object> operatorInfo =
				operatorsQueryService.getInfo(Map.of("company_id", companyId, "operator_id", targetOperatorId));
		if (operatorInfo == null || operatorInfo.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		String name = operatorInfo.get("username") == null ? "" : String.valueOf(operatorInfo.get("username"));

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("is_disable", isDisableQuery);
		logParams.put("name", name);
		long relMerchantId = jwtOperatorId;
		long relDealerId;
		if (operatorInfo.containsKey("is_dealer_main")
				&& isDealerMainFalsy(operatorInfo.get("is_dealer_main"))) {
			relDealerId = parseLongSafe(operatorInfo.get("dealer_parent_id"), targetOperatorId);
		} else {
			relDealerId = targetOperatorId;
		}
		adapayOperationLogRecordPort.logRecord(logParams, relMerchantId, "dealer/disable", "merchant", jwtOperatorId);
		adapayOperationLogRecordPort.logRecord(logParams, relDealerId, "dealer/disable", "dealer", jwtOperatorId);
	}

	private List<Map<String, Object>> parseDistributorIdsJson(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (!node.isArray()) {
				return Collections.emptyList();
			}
			return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
	}

	private static boolean isDisableTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(String.valueOf(raw).trim());
	}

	private static boolean isDealerMainFalsy(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 0;
		}
		String s = String.valueOf(raw).trim();
		if ("0".equals(s)) {
			return true;
		}
		if ("1".equals(s)) {
			return false;
		}
		return false;
	}

	private static long parseLongSafe(Object value, long fallback) {
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
