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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ServiceLabels;
import cn.shopex.ecshopx.goods.mapper.ServiceLabelsMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ServiceLabelsCreateService {

	private static final String VALIDATION_MSG = "添加会员数值属性出错.";

	private static final String UPDATE_VALIDATION_MSG = "更新会员数值属性出错.";

	private final ServiceLabelsMapper mapper;

	private static BadRequestException validationFailed(String message, String field, String validationRuleCode) {
		LinkedHashMap<String, List<String>> errors = new LinkedHashMap<>();
		errors.put(field, List.of(validationRuleCode));
		return new BadRequestException(message, errors);
	}

	private static BadRequestException validationFailed(String field, String validationRuleCode) {
		return validationFailed(VALIDATION_MSG, field, validationRuleCode);
	}

	public ServiceLabelsCreateService(ServiceLabelsMapper mapper) {
		this.mapper = mapper;
	}

	private static Map<String, Object> toDetailResponseMap(ServiceLabels row) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("label_id", row.getLabelId());
		out.put("label_name", row.getLabelName());
		out.put("label_price", row.getLabelPrice());
		out.put("label_desc", row.getLabelDesc());
		out.put("service_type", row.getServiceType());
		out.put("company_id", row.getCompanyId());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}

	public Map<String, Object> getServiceLabelsDetail(long companyId, long labelId) {
		ServiceLabels row = mapper.selectById(labelId);
		if (row == null) {
			throw new ResourceException("label_id=" + labelId + "的会员数值属性不存在");
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			throw new ResourceException("获取会员数值属性信息有误，请确认您的ID.");
		}
		return toDetailResponseMap(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createServiceLabels(long companyId, Map<String, Object> input) {
		Object nameObj = input.get("label_name");
		if (nameObj == null || !StringUtils.hasText(nameObj.toString().trim())) {
			throw validationFailed("label_name", "validation.required");
		}
		String labelName = nameObj.toString().trim();

		Object priceObj = input.get("label_price");
		if (priceObj == null || !StringUtils.hasText(priceObj.toString().trim())) {
			throw validationFailed("label_price", "validation.required");
		}
		String priceStr = priceObj.toString().trim();
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(priceStr);
		} catch (NumberFormatException e) {
			throw validationFailed("label_price", "validation.numeric");
		}
		if (yuan.compareTo(BigDecimal.ZERO) < 0) {
			throw validationFailed("label_price", "validation.min.numeric");
		}
		int priceFen;
		try {
			priceFen = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
		} catch (ArithmeticException e) {
			throw validationFailed("label_price", "validation.numeric");
		}

		Object stObj = input.get("service_type");
		if (stObj == null || !StringUtils.hasText(stObj.toString().trim())) {
			throw validationFailed("service_type", "validation.required");
		}
		String serviceType = stObj.toString().trim();
		if (!"point".equals(serviceType) && !"deposit".equals(serviceType) && !"timescard".equals(serviceType)) {
			throw validationFailed("service_type", "validation.in");
		}

		String labelDesc;
		if (!input.containsKey("label_desc") || input.get("label_desc") == null) {
			labelDesc = null;
		} else {
			Object v = input.get("label_desc");
			if (v instanceof String s) {
				labelDesc = s;
			} else {
				labelDesc = String.valueOf(v);
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		ServiceLabels entity = new ServiceLabels();
		entity.setLabelName(labelName);
		entity.setLabelPrice(priceFen);
		entity.setLabelDesc(labelDesc);
		entity.setServiceType(serviceType);
		entity.setCompanyId(companyId);
		entity.setCreated(now);
		entity.setUpdated(now);

		if (entity.getLabelDesc() == null) {
			entity.setLabelDesc("");
		}

		mapper.insert(entity);
		if (entity.getLabelId() == null) {
			throw new ResourceException("创建数值属性失败");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("label_id", entity.getLabelId());
		out.put("label_name", entity.getLabelName());
		out.put("label_desc", entity.getLabelDesc());
		out.put("service_type", entity.getServiceType());
		out.put("label_price", entity.getLabelPrice());
		out.put("company_id", entity.getCompanyId());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateServiceLabels(long companyId, long labelId, Map<String, Object> input) {
		Object nameObj = input.get("label_name");
		if (nameObj == null || !StringUtils.hasText(nameObj.toString().trim())) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "label_name", "validation.required");
		}
		String labelName = nameObj.toString().trim();

		Object priceObj = input.get("label_price");
		if (priceObj == null || !StringUtils.hasText(priceObj.toString().trim())) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "label_price", "validation.required");
		}
		String priceStr = priceObj.toString().trim();
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(priceStr);
		} catch (NumberFormatException e) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "label_price", "validation.numeric");
		}
		if (yuan.compareTo(BigDecimal.ZERO) < 0) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "label_price", "validation.min.numeric");
		}
		int priceFen;
		try {
			priceFen = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
		} catch (ArithmeticException e) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "label_price", "validation.numeric");
		}

		Object stObj = input.get("service_type");
		if (stObj == null || !StringUtils.hasText(stObj.toString().trim())) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "service_type", "validation.required");
		}
		String serviceType = stObj.toString().trim();
		if (!"point".equals(serviceType) && !"deposit".equals(serviceType) && !"timescard".equals(serviceType)) {
			throw validationFailed(UPDATE_VALIDATION_MSG, "service_type", "validation.in");
		}

		String labelDesc;
		if (!input.containsKey("label_desc") || input.get("label_desc") == null) {
			labelDesc = null;
		} else {
			Object v = input.get("label_desc");
			if (v instanceof String s) {
				labelDesc = s;
			} else {
				labelDesc = String.valueOf(v);
			}
		}

		ServiceLabels row = mapper.selectById(labelId);
		if (row == null) {
			throw new ResourceException("label_id=" + labelId + "的会员数值属性不存在");
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			throw new ResourceException("请确认您的会员数值属性信息后再提交.");
		}

		row.setLabelName(labelName);
		row.setLabelPrice(priceFen);
		row.setLabelDesc(labelDesc == null ? "" : labelDesc);
		row.setServiceType(serviceType);
		int now = (int) (System.currentTimeMillis() / 1000L);
		row.setUpdated(now);

		mapper.updateById(row);

		return toDetailResponseMap(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteServiceLabels(long companyId, long labelId) {
		ServiceLabels row = mapper.selectById(labelId);
		if (row == null) {
			throw new ResourceException("label_id=" + labelId + "的会员数值属性不存在");
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			throw new ResourceException("删除会员数值属性信息有误.");
		}
		int affected = mapper.deleteById(labelId);
		if (affected == 0) {
			throw new ResourceException("label_id=" + labelId + "的会员数值属性不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", null);
		return out;
	}
}
