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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatMessageTemplate;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatMessageTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkWechatMessageTemplateService {

	private static final Set<String> TEMPLATE_IDS = Set.of(
			"waitingDeliveryNotice",
			"salespersonTaskNotice",
			"completeTaskNotice");

	private static final List<String> FULL_BRANCH_STRING_FIELDS = List.of("title", "description");

	private final WorkWechatMessageTemplateMapper workWechatMessageTemplateMapper;
	private final ObjectMapper objectMapper;

	public WorkWechatMessageTemplateService(
			WorkWechatMessageTemplateMapper workWechatMessageTemplateMapper,
			ObjectMapper objectMapper) {
		this.workWechatMessageTemplateMapper = workWechatMessageTemplateMapper;
		this.objectMapper = objectMapper;
	}

	public Object getTemplateForApi(long companyId, String templateId) {
		LambdaQueryWrapper<WorkWechatMessageTemplate> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WorkWechatMessageTemplate::getCompanyId, companyId)
				.eq(WorkWechatMessageTemplate::getTemplateId, templateId);
		WorkWechatMessageTemplate entity = workWechatMessageTemplateMapper.selectOne(wrapper);
		if (entity == null) {
			return Collections.emptyList();
		}
		Map<String, Object> row = toColumnNamesData(entity);
		applyParsedContentForDoctrineStyleApi(row);
		return row;
	}

	public Map<String, Object> listTemplatesGroupedByTemplateId(long companyId) {
		LambdaQueryWrapper<WorkWechatMessageTemplate> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WorkWechatMessageTemplate::getCompanyId, companyId);
		List<WorkWechatMessageTemplate> rows = workWechatMessageTemplateMapper.selectList(wrapper);
		if (rows == null || rows.isEmpty()) {
			return new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (WorkWechatMessageTemplate entity : rows) {
			Map<String, Object> rowMap = toColumnNamesData(entity);
			Object rawId = rowMap.get("templateId");
			if (rawId == null || !(rawId instanceof String) || ((String) rawId).isEmpty()) {
				continue;
			}
			out.put((String) rawId, rowMap);
		}
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveTemplate(long companyId, String templateId, Map<String, Object> params) {
		if (!TEMPLATE_IDS.contains(templateId)) {
			throw new ResourceException("没有该通知模版类型");
		}
		boolean titleSet = params.containsKey("title") && params.get("title") != null;
		if (!titleSet) {
			return partialUpdate(companyId, templateId, params);
		}
		return fullSave(companyId, templateId, params);
	}

	private Map<String, Object> partialUpdate(long companyId, String templateId, Map<String, Object> params) {
		boolean disabledFlag = isTruthyDisabledString(params.get("disabled"));
		LambdaQueryWrapper<WorkWechatMessageTemplate> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WorkWechatMessageTemplate::getCompanyId, companyId)
				.eq(WorkWechatMessageTemplate::getTemplateId, templateId);
		WorkWechatMessageTemplate row = workWechatMessageTemplateMapper.selectOne(wrapper);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		row.setDisabled(disabledFlag);
		int rows = workWechatMessageTemplateMapper.updateById(row);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return new LinkedHashMap<>(params);
	}

	private Map<String, Object> fullSave(long companyId, String templateId, Map<String, Object> params) {
		validateFullBranchStrings(params);
		if (!params.containsKey("disabled") || params.get("disabled") == null) {
			throw new BadRequestException("模版开启规则必填");
		}
		if (!params.containsKey("emphasis_first_item") || params.get("emphasis_first_item") == null) {
			throw new BadRequestException("是否放大第一个内容");
		}
		boolean disabledFlag = isStringTrue(params.get("disabled"));
		boolean emphasisFirst = isStringTrue(params.get("emphasis_first_item"));
		String title = ((String) params.get("title")).trim();
		String description = ((String) params.get("description")).trim();
		boolean shouldWriteContent = params.containsKey("content") && params.get("content") != null;
		String contentJson = null;
		if (shouldWriteContent) {
			contentJson = resolveContentJsonString(params.get("content"));
		}
		LambdaQueryWrapper<WorkWechatMessageTemplate> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WorkWechatMessageTemplate::getCompanyId, companyId)
				.eq(WorkWechatMessageTemplate::getTemplateId, templateId);
		WorkWechatMessageTemplate existing = workWechatMessageTemplateMapper.selectOne(wrapper);
		if (existing == null) {
			WorkWechatMessageTemplate row = new WorkWechatMessageTemplate();
			row.setCompanyId(companyId);
			row.setTemplateId(templateId);
			row.setDisabled(disabledFlag);
			row.setEmphasisFirstItem(emphasisFirst);
			row.setTitle(title);
			row.setDescription(description);
			if (shouldWriteContent) {
				row.setContent(contentJson);
			}
			workWechatMessageTemplateMapper.insert(row);
			Map<String, Object> inserted = toColumnNamesData(row);
			applyParsedContentForDoctrineStyleApi(inserted);
			return inserted;
		}
		existing.setTitle(title);
		existing.setDescription(description);
		existing.setDisabled(disabledFlag);
		existing.setEmphasisFirstItem(emphasisFirst);
		if (shouldWriteContent) {
			existing.setContent(contentJson);
		}
		workWechatMessageTemplateMapper.updateById(existing);
		Map<String, Object> updated = toColumnNamesData(existing);
		applyParsedContentForDoctrineStyleApi(updated);
		return updated;
	}

	private void validateFullBranchStrings(Map<String, Object> params) {
		for (String field : FULL_BRANCH_STRING_FIELDS) {
			Object raw = params.get(field);
			String message = field.equals("title")
					? "企业微信通知模板标题长度在4-12个字符"
					: "企业微信通知模板内容长度在4-12个字符";
			if (!(raw instanceof String s)) {
				throw new BadRequestException(message);
			}
			s = s.trim();
			if (s.length() < 4 || s.length() > 12) {
				throw new BadRequestException(message);
			}
		}
	}

	private String resolveContentJsonString(Object raw) {
		if (raw instanceof String s) {
			try {
				objectMapper.readTree(s);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("通知模板内容格式错误");
			}
			return s;
		}
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			try {
				return objectMapper.writeValueAsString(raw);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("通知模板内容格式错误");
			}
		}
		throw new BadRequestException("通知模板内容格式错误");
	}

	private void applyParsedContentForDoctrineStyleApi(Map<String, Object> row) {
		if (row == null) {
			return;
		}
		Object content = row.get("content");
		if (content == null) {
			return;
		}
		if (content instanceof String s) {
			if (s.isEmpty()) {
				return;
			}
			try {
				row.put("content", objectMapper.readValue(s, Object.class));
			} catch (JsonProcessingException e) {
				throw new ResourceException("通知模板内容数据异常");
			}
			return;
		}
		if (content instanceof Map<?, ?> || content instanceof List<?>) {
			return;
		}
	}

	private Map<String, Object> toColumnNamesData(WorkWechatMessageTemplate entity) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("id", entity.getId());
		map.put("company_id", entity.getCompanyId());
		map.put("template_id", entity.getTemplateId());
		map.put("templateId", entity.getTemplateId());
		map.put("disabled", entity.getDisabled());
		map.put("emphasis_first_item", entity.getEmphasisFirstItem());
		map.put("title", entity.getTitle());
		map.put("description", entity.getDescription());
		map.put("content", normalizeTemplateContentForApi(entity.getContent()));
		return map;
	}

	/**
	 * 将模板 content 规范为 API 对外使用的 JSON 字符串：已为字符串则按需整理；Map/List 等结构则序列化为字符串。
	 */
	private String normalizeTemplateContentForApi(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s.trim().isEmpty() ? null : s;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new ResourceException("通知模板内容数据异常");
		}
	}

	private static boolean isTruthyDisabledString(Object v) {
		return Objects.equals("true", Objects.toString(v, "").trim());
	}

	private static boolean isStringTrue(Object v) {
		return Objects.equals("true", Objects.toString(v, "").trim());
	}

}
