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

package cn.shopex.ecshopx.orders.service.companyreldelivery;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CompanyRelDelivery;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompanyRelDeliveryAdminSaveService {

	private final CompanyRelDeliveryMapper companyRelDeliveryMapper;

	private final ObjectMapper objectMapper;

	public CompanyRelDeliveryAdminSaveService(
			CompanyRelDeliveryMapper companyRelDeliveryMapper, ObjectMapper objectMapper) {
		this.companyRelDeliveryMapper = companyRelDeliveryMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getInfo(long companyId) {
		CompanyRelDelivery row = companyRelDeliveryMapper.selectOne(new LambdaQueryWrapper<CompanyRelDelivery>()
				.eq(CompanyRelDelivery::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (row == null) {
			LinkedHashMap<String, Object> sparse = new LinkedHashMap<>();
			sparse.put("rules", new ArrayList<>());
			sparse.put("other_params", new ArrayList<>());
			return sparse;
		}
		return handlerData(row, true);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> save(long companyId, Map<String, Object> merged) {
		Object rawType = merged.get("type");
		if (rawType == null) {
			throw new BadRequestException("类型必填！");
		}
		int typeInt = (int) longLoose(rawType);

		Integer statusPatch = null;
		if (merged.containsKey("status")) {
			Object rawStatus = merged.get("status");
			if (rawStatus != null) {
				statusPatch = (int) longLoose(rawStatus);
			}
		}

		Object rawFreight = merged.get("freight");
		String freightStr = rawFreight == null ? "" : String.valueOf(rawFreight);
		String freightNorm = freightStr.trim();
		if (!isDecimalNumeric(freightNorm)) {
			freightNorm = "0";
		}

		Map<String, String> ruleObj = new LinkedHashMap<>();
		ruleObj.put("freight_price", freightNorm);
		String rulesJson;
		try {
			rulesJson = objectMapper.writeValueAsString(ruleObj);
		} catch (JsonProcessingException e) {
			throw new ResourceException("规则数据生成失败");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<CompanyRelDelivery> uw = new LambdaUpdateWrapper<>();
		uw.eq(CompanyRelDelivery::getCompanyId, companyId);
		uw.set(CompanyRelDelivery::getType, typeInt);
		uw.set(CompanyRelDelivery::getRules, rulesJson);
		uw.set(CompanyRelDelivery::getUpdated, now);
		if (statusPatch != null) {
			uw.set(CompanyRelDelivery::getStatus, statusPatch);
		}
		int n = companyRelDeliveryMapper.update(null, uw);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		CompanyRelDelivery row = companyRelDeliveryMapper.selectOne(new LambdaQueryWrapper<CompanyRelDelivery>()
				.eq(CompanyRelDelivery::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return handlerData(row, false);
	}

	private Map<String, Object> handlerData(CompanyRelDelivery entity, boolean serializeTypeAsBoolean) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("company_id", entity.getCompanyId());
		Integer typeVal = entity.getType();
		if (serializeTypeAsBoolean) {
			out.put("type", typeVal != null && typeVal != 0);
		} else {
			out.put("type", typeVal);
		}
		out.put("status", entity.getStatus());
		out.put("rules", decodeAssocJsonField(entity.getRules()));
		out.put("other_params", decodeAssocJsonField(entity.getOtherParams()));
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}

	/** Empty JSON object or blank stored value serializes as an empty array; non-empty objects become maps. */
	private Object decodeAssocJsonField(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			JsonNode root = objectMapper.readTree(json);
			return unfoldJsonContainer(root);
		} catch (JsonProcessingException e) {
			return new ArrayList<>();
		}
	}

	private Object unfoldJsonContainer(JsonNode node) {
		if (node == null || node.isNull()) {
			return new ArrayList<>();
		}
		if (node.isArray()) {
			List<Object> list = new ArrayList<>();
			for (JsonNode el : node) {
				list.add(unfoldJsonValue(el));
			}
			return list;
		}
		if (node.isObject()) {
			if (node.isEmpty()) {
				return new ArrayList<>();
			}
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			node.fields().forEachRemaining(e -> map.put(e.getKey(), unfoldJsonValue(e.getValue())));
			return map;
		}
		return jsonLeaf(node);
	}

	private Object unfoldJsonValue(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isArray() || node.isObject()) {
			return unfoldJsonContainer(node);
		}
		return jsonLeaf(node);
	}

	private static Object jsonLeaf(JsonNode node) {
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isInt()) {
			return node.intValue();
		}
		if (node.isLong()) {
			return node.longValue();
		}
		if (node.isFloatingPointNumber() || node.isBigDecimal()) {
			return node.doubleValue();
		}
		if (node.isTextual()) {
			return node.asText();
		}
		if (node.isNumber()) {
			return node.numberValue();
		}
		return node.asText();
	}

	private static long longLoose(Object raw) {
		if (raw instanceof Number) {
			return ((Number) raw).longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}

	private static boolean isDecimalNumeric(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		try {
			new BigDecimal(s.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
