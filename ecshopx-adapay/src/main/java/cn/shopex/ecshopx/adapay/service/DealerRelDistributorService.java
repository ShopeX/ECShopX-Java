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

import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerRelDistributorService {

	private final AdapayMemberMapper adapayMemberMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final OperatorsMapper operatorsMapper;
	private final DistributorMapper distributorMapper;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;
	private final ObjectMapper objectMapper;

	public DealerRelDistributorService(
			AdapayMemberMapper adapayMemberMapper,
			OperatorsQueryService operatorsQueryService,
			OperatorsMapper operatorsMapper,
			DistributorMapper distributorMapper,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort,
			ObjectMapper objectMapper) {
		this.adapayMemberMapper = adapayMemberMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.operatorsMapper = operatorsMapper;
		this.distributorMapper = distributorMapper;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
		this.objectMapper = objectMapper;
	}

	public void dealerRelDistributor(
			long companyId,
			long jwtOperatorId,
			String operatorIdRaw,
			String distributorIdRaw,
			String nameRaw,
			String isRelRaw,
			String headquartersProportionRaw,
			String dealerProportionRaw) {
		if (operatorIdRaw == null || operatorIdRaw.trim().isEmpty()) {
			throw new BadRequestException("operator_id 格式不正确");
		}
		if (distributorIdRaw == null || distributorIdRaw.trim().isEmpty()) {
			throw new BadRequestException("distributor_id 格式不正确");
		}
		long dealerOperatorId;
		try {
			dealerOperatorId = Long.parseLong(operatorIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("operator_id 格式不正确");
		}
		long distributorId;
		try {
			distributorId = Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 格式不正确");
		}
		if (distributorId > Integer.MAX_VALUE || distributorId < Integer.MIN_VALUE) {
			throw new BadRequestException("distributor_id 格式不正确");
		}
		if (dealerOperatorId > Integer.MAX_VALUE || dealerOperatorId < Integer.MIN_VALUE) {
			throw new BadRequestException("operator_id 格式不正确");
		}

		double hq = parseProportionOrZero(headquartersProportionRaw, "headquarters_proportion");
		double dealerPct = parseProportionOrZero(dealerProportionRaw, "dealer_proportion");
		String nameForJson = nameRaw == null ? "" : nameRaw.trim();

		LambdaQueryWrapper<AdapayMember> memberW = new LambdaQueryWrapper<>();
		memberW.eq(AdapayMember::getCompanyId, companyId)
				.eq(AdapayMember::getOperatorId, (int) distributorId)
				.eq(AdapayMember::getOperatorType, "distributor")
				.eq(AdapayMember::getAuditState, "E");
		AdapayMember memberRow = adapayMemberMapper.selectOne(memberW);
		boolean hasApprovedMember = memberRow != null;

		Map<String, Object> operatorInfo =
				operatorsQueryService.getInfo(
						Map.of("company_id", companyId, "operator_id", dealerOperatorId));
		if (operatorInfo == null || operatorInfo.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}

		Distributor distributorRow =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.eq(Distributor::getDistributorId, distributorId));
		if (distributorRow == null) {
			throw new ResourceException("未查询到更新数据");
		}

		boolean rel = isRelTruthy(isRelRaw);
		int dealerIdInt = (int) dealerOperatorId;

		if (rel) {
			if (hasApprovedMember) {
				if (hq + dealerPct > 100.0) {
					throw new ResourceException("分账占比设置必须小于等于 100 %");
				}
				Map<String, Object> split = parseSplitLedgerJsonOrEmpty(distributorRow.getSplitLedgerInfo());
				String hqNode =
						headquartersProportionRaw == null ? "" : headquartersProportionRaw.trim();
				String dealerNode = dealerProportionRaw == null ? "" : dealerProportionRaw.trim();
				split.put("headquarters_proportion", hqNode);
				split.put("dealer_proportion", dealerNode);
				String newSplitJson = writeJsonUnchecked(split);
				LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
				u.eq(Distributor::getCompanyId, companyId)
						.eq(Distributor::getDistributorId, distributorId)
						.set(Distributor::getDealerId, dealerIdInt)
						.set(Distributor::getSplitLedgerInfo, newSplitJson);
				distributorMapper.update(null, u);
			} else {
				LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
				u.eq(Distributor::getCompanyId, companyId)
						.eq(Distributor::getDistributorId, distributorId)
						.set(Distributor::getDealerId, dealerIdInt);
				distributorMapper.update(null, u);
			}

			List<Map<String, Object>> existing =
					parseDistributorIdsFromOperatorInfo(operatorInfo.get("distributor_ids"));
			List<Map<String, Object>> newList =
					existing.isEmpty()
							? new ArrayList<>(List.of(buildDistributorIdsEntry(nameForJson, distributorId)))
							: appendDistributorEntry(new ArrayList<>(existing), nameForJson, distributorId);
			persistOperatorDistributorIds(companyId, dealerOperatorId, newList);
		} else {
			if (hasApprovedMember) {
				if (hq > 100.0) {
					throw new ResourceException("分账占比设置必须小于等于 100 %");
				}
				Map<String, Object> split = parseSplitLedgerJsonOrEmpty(distributorRow.getSplitLedgerInfo());
				String hqNode =
						headquartersProportionRaw == null ? "" : headquartersProportionRaw.trim();
				split.put("headquarters_proportion", hqNode);
				split.put("dealer_proportion", "");
				String newSplitJson = writeJsonUnchecked(split);
				LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
				u.eq(Distributor::getCompanyId, companyId)
						.eq(Distributor::getDistributorId, distributorId)
						.set(Distributor::getDealerId, 0)
						.set(Distributor::getSplitLedgerInfo, newSplitJson);
				distributorMapper.update(null, u);
			} else {
				LambdaUpdateWrapper<Distributor> u = new LambdaUpdateWrapper<>();
				u.eq(Distributor::getCompanyId, companyId)
						.eq(Distributor::getDistributorId, distributorId)
						.set(Distributor::getDealerId, 0);
				distributorMapper.update(null, u);
			}

			List<Map<String, Object>> existing =
					parseDistributorIdsFromOperatorInfo(operatorInfo.get("distributor_ids"));
			List<Map<String, Object>> filtered = removeDistributorEntries(existing, distributorId);
			persistOperatorDistributorIds(companyId, dealerOperatorId, filtered);
		}

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("is_rel", rel);
		logParams.put("dealer_name", String.valueOf(operatorInfo.getOrDefault("username", "")));
		logParams.put(
				"distributor_name", distributorRow.getName() != null ? distributorRow.getName() : "");

		long relDealerId;
		if (operatorInfo.containsKey("is_dealer_main")
				&& isDealerMainFalsy(operatorInfo.get("is_dealer_main"))) {
			relDealerId = parseLongSafe(operatorInfo.get("dealer_parent_id"), dealerOperatorId);
		} else {
			relDealerId = dealerOperatorId;
		}

		adapayOperationLogRecordPort.logRecord(
				logParams, distributorId, "dealer/rel/distributor", "distributor", jwtOperatorId);
		adapayOperationLogRecordPort.logRecord(
				logParams, relDealerId, "dealer/rel/dealer", "dealer", jwtOperatorId);
	}

	private void persistOperatorDistributorIds(long companyId, long dealerOperatorId, List<Map<String, Object>> list) {
		Operators row =
				operatorsMapper.selectOne(
						new LambdaQueryWrapper<Operators>()
								.eq(Operators::getCompanyId, companyId)
								.eq(Operators::getOperatorId, dealerOperatorId));
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		row.setDistributorIds(writeJsonUnchecked(list));
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		int n = operatorsMapper.updateById(row);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private String writeJsonUnchecked(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据序列化失败");
		}
	}

	private Map<String, Object> parseSplitLedgerJsonOrEmpty(String json) {
		if (json == null || json.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			JsonNode node = objectMapper.readTree(json);
			if (!node.isObject()) {
				return new LinkedHashMap<>();
			}
			return objectMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (JsonProcessingException e) {
			return new LinkedHashMap<>();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseDistributorIdsFromOperatorInfo(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					out.add((Map<String, Object>) m);
				}
			}
			return out;
		}
		if (raw instanceof String s) {
			return parseDistributorIdsFromJsonString(s);
		}
		return new ArrayList<>();
	}

	private List<Map<String, Object>> parseDistributorIdsFromJsonString(String json) {
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

	private static Map<String, Object> buildDistributorIdsEntry(String nameForJson, long distributorId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", nameForJson);
		m.put("distributor_id", String.valueOf(distributorId));
		return m;
	}

	private static List<Map<String, Object>> appendDistributorEntry(
			List<Map<String, Object>> list, String nameForJson, long distributorId) {
		list.add(buildDistributorIdsEntry(nameForJson, distributorId));
		return list;
	}

	private static List<Map<String, Object>> removeDistributorEntries(
			List<Map<String, Object>> list, long distributorId) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> item : list) {
			Object idObj = item.get("distributor_id");
			if (!distributorIdMatches(idObj, distributorId)) {
				out.add(item);
			}
		}
		return out;
	}

	private static boolean distributorIdMatches(Object idObj, long distributorId) {
		if (idObj == null) {
			return false;
		}
		if (idObj instanceof Number n) {
			return n.longValue() == distributorId;
		}
		return String.valueOf(idObj).trim().equals(String.valueOf(distributorId));
	}

	private static double parseProportionOrZero(String raw, String fieldName) {
		if (raw == null || raw.trim().isEmpty()) {
			return 0.0;
		}
		try {
			return Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(fieldName + " 格式不正确");
		}
	}

	private static boolean isRelTruthy(String isRelRaw) {
		if (isRelRaw == null) {
			return false;
		}
		String s = isRelRaw.trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		return true;
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
