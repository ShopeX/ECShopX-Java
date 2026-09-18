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

package cn.shopex.ecshopx.crossborder.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.crossborder.domain.Taxstrategy;
import cn.shopex.ecshopx.crossborder.mapper.TaxstrategyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxstrategyInfoQueryService {

	private final TaxstrategyMapper taxstrategyMapper;
	private final ObjectMapper objectMapper;

	public TaxstrategyInfoQueryService(TaxstrategyMapper taxstrategyMapper, ObjectMapper objectMapper) {
		this.taxstrategyMapper = taxstrategyMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInfo(long companyId, long taxstrategyId) {
		LambdaQueryWrapper<Taxstrategy> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Taxstrategy::getId, taxstrategyId);
		wrapper.eq(Taxstrategy::getCompanyId, companyId);
		wrapper.eq(Taxstrategy::getState, 1);
		Taxstrategy row = taxstrategyMapper.selectOne(wrapper);
		if (row == null) {
			throw new ResourceException("操作失败");
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", row.getCompanyId());
		data.put("id", row.getId());
		data.put("taxstrategy_name", row.getTaxstrategyName());
		data.put("state", row.getState());
		data.put("created", row.getCreated());
		data.put("updated", row.getUpdated());

		String raw = row.getTaxstrategyContent();
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new ResourceException("操作失败");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new ResourceException("操作失败");
		}
		if (!root.isArray()) {
			throw new ResourceException("操作失败");
		}

		List<Object> contentList = new ArrayList<>();
		for (JsonNode el : root) {
			if (el.isTextual()) {
				try {
					JsonNode inner = objectMapper.readTree(el.asText());
					contentList.add(objectMapper.convertValue(inner, Object.class));
				} catch (JsonProcessingException e) {
					throw new ResourceException("操作失败");
				}
			} else if (el.isObject() || el.isArray()) {
				contentList.add(objectMapper.convertValue(el, Object.class));
			} else {
				throw new ResourceException("操作失败");
			}
		}
		data.put("taxstrategy_content", contentList);
		return data;
	}
}
