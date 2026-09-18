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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormTemplateCreateRequest;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FormTemplateCreateService {

	private final FormTemplateMapper formTemplateMapper;
	private final FormTemplateOutsideMultiLangWriteService multiLangWriteService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;
	private final FormTemplateApiRowAssembler formTemplateApiRowAssembler;

	public FormTemplateCreateService(
			FormTemplateMapper formTemplateMapper,
			FormTemplateOutsideMultiLangWriteService multiLangWriteService,
			ObjectMapper objectMapper,
			MessageSource messageSource,
			FormTemplateApiRowAssembler formTemplateApiRowAssembler) {
		this.formTemplateMapper = formTemplateMapper;
		this.multiLangWriteService = multiLangWriteService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
		this.formTemplateApiRowAssembler = formTemplateApiRowAssembler;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createData(FormTemplateCreateRequest request, long companyId, String requestLangTag) {
		if (!StringUtils.hasText(request.getTemName())) {
			throw new BadRequestException("tem_name必填");
		}
		if (!StringUtils.hasText(request.getTemType())) {
			throw new BadRequestException("tem_type必填");
		}
		JsonNode c = request.getContent();
		if (c == null || c.isNull()) {
			throw new BadRequestException("content必填");
		}
		JsonNode normalizedContentNode;
		if (c.isTextual()) {
			try {
				normalizedContentNode = objectMapper.readTree(c.asText());
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("content JSON 无效");
			}
		} else {
			normalizedContentNode = c;
		}
		if (normalizedContentNode == null || normalizedContentNode.isNull()) {
			throw new BadRequestException("content必填");
		}

		JsonNode normalizedKeyIndex;
		if (request.getKeyIndex() == null || request.getKeyIndex().isNull()) {
			normalizedKeyIndex = null;
		} else if (request.getKeyIndex().isTextual()) {
			try {
				normalizedKeyIndex = objectMapper.readTree(request.getKeyIndex().asText());
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("key_index JSON 无效");
			}
		} else {
			normalizedKeyIndex = request.getKeyIndex();
		}

		FormTemplate entity = new FormTemplate();
		entity.setCompanyId(companyId);
		entity.setTemName(request.getTemName().trim());
		if (request.getHeaderHeight() != null) {
			entity.setHeaderHeight(request.getHeaderHeight());
		}
		if (request.getHeaderBgPic() != null) {
			entity.setHeaderBgPic(request.getHeaderBgPic());
		}
		try {
			entity.setContent(objectMapper.writeValueAsString(normalizedContentNode));
		} catch (JsonProcessingException ex) {
			throw new ResourceException("创建表单模板失败");
		}
		if (StringUtils.hasText(request.getStatus())) {
			int parsed;
			try {
				parsed = Integer.parseInt(request.getStatus().trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("status 无效");
			}
			if (parsed != 0) {
				entity.setStatus(parsed);
			}
		}
		entity.setTemType(request.getTemType().trim());
		if (normalizedKeyIndex != null && !normalizedKeyIndex.isNull()) {
			boolean emptyArray = normalizedKeyIndex.isArray() && normalizedKeyIndex.size() == 0;
			boolean emptyObject = normalizedKeyIndex.isObject() && !normalizedKeyIndex.fieldNames().hasNext();
			if (!emptyArray && !emptyObject) {
				try {
					entity.setKeyIndex(objectMapper.writeValueAsString(normalizedKeyIndex));
				} catch (JsonProcessingException ex) {
					throw new ResourceException("创建表单模板失败");
				}
			}
		}
		if (StringUtils.hasText(request.getFormStyle())) {
			entity.setFormStyle(request.getFormStyle());
		}
		if (request.getHeaderLinkTitle() != null) {
			entity.setHeaderLinkTitle(request.getHeaderLinkTitle());
		}
		if (request.getHeaderTitle() != null) {
			entity.setHeaderTitle(request.getHeaderTitle());
		}
		if (request.getBottomTitle() != null) {
			entity.setBottomTitle(request.getBottomTitle());
		}

		int ts = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(ts);
		entity.setUpdated(ts);

		formTemplateMapper.insert(entity);
		if (entity.getId() == null || entity.getId() <= 0L) {
			throw new ResourceException("创建表单模板失败");
		}

		Map<String, Object> rawForLang = new LinkedHashMap<>();
		rawForLang.put("tem_name", request.getTemName());
		rawForLang.put("content", normalizedContentNode);
		rawForLang.put("header_link_title", request.getHeaderLinkTitle());
		rawForLang.put("header_bg_pic", request.getHeaderBgPic());
		rawForLang.put("header_title", request.getHeaderTitle());
		multiLangWriteService.addForNewFormTemplate(entity.getId(), companyId, rawForLang, requestLangTag);

		return formTemplateApiRowAssembler.toRow(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateData(
			FormTemplateCreateRequest request, long companyId, String requestLangTag, Locale locale) {
		if (!StringUtils.hasText(request.getId())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_template.tag_id_required", null, locale));
		}
		if (!StringUtils.hasText(request.getTemName())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_template.field_title_required", null, locale));
		}
		JsonNode c = request.getContent();
		if (c == null || c.isNull()) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_template.field_name_required", null, locale));
		}
		JsonNode normalizedContentNode;
		if (c.isTextual()) {
			try {
				normalizedContentNode = objectMapper.readTree(c.asText());
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("content JSON 无效");
			}
		} else {
			normalizedContentNode = c;
		}
		if (normalizedContentNode == null || normalizedContentNode.isNull()) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_template.field_name_required", null, locale));
		}

		JsonNode normalizedKeyIndex;
		if (request.getKeyIndex() == null || request.getKeyIndex().isNull()) {
			normalizedKeyIndex = null;
		} else if (request.getKeyIndex().isTextual()) {
			try {
				normalizedKeyIndex = objectMapper.readTree(request.getKeyIndex().asText());
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("key_index JSON 无效");
			}
		} else {
			normalizedKeyIndex = request.getKeyIndex();
		}

		long rowId;
		try {
			rowId = Long.parseLong(request.getId().trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_template.tag_id_required", null, locale));
		}
		if (rowId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_template.tag_id_required", null, locale));
		}

		LambdaQueryWrapper<FormTemplate> w = new LambdaQueryWrapper<>();
		w.eq(FormTemplate::getId, rowId).eq(FormTemplate::getCompanyId, companyId);
		FormTemplate entity = formTemplateMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.form_template.no_update_data_found", null, locale));
		}

		applyColumnUpdateFromRequest(entity, request, normalizedContentNode, normalizedKeyIndex);

		entity.setUpdated((int) (System.currentTimeMillis() / 1000L));
		int affected = formTemplateMapper.updateById(entity);
		if (affected == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.form_template.no_update_data_found", null, locale));
		}

		Map<String, Object> langBag = new LinkedHashMap<>();
		if (StringUtils.hasText(request.getTemName())) {
			langBag.put("tem_name", request.getTemName().trim());
		}
		langBag.put("content", normalizedContentNode);
		if (request.getHeaderLinkTitle() != null) {
			langBag.put("header_link_title", request.getHeaderLinkTitle());
		}
		if (request.getHeaderBgPic() != null) {
			langBag.put("header_bg_pic", request.getHeaderBgPic());
		}
		if (request.getHeaderTitle() != null) {
			langBag.put("header_title", request.getHeaderTitle());
		}
		multiLangWriteService.updateLangDataForFormTemplate(
				entity.getId(), companyId, langBag, requestLangTag);

		return formTemplateApiRowAssembler.toRow(entity);
	}

	private void applyColumnUpdateFromRequest(
			FormTemplate entity,
			FormTemplateCreateRequest request,
			JsonNode normalizedContentNode,
			JsonNode normalizedKeyIndex) {
		if (StringUtils.hasText(request.getTemName())) {
			entity.setTemName(request.getTemName().trim());
		}
		if (request.getHeaderHeight() != null) {
			entity.setHeaderHeight(request.getHeaderHeight());
		}
		if (request.getHeaderBgPic() != null) {
			entity.setHeaderBgPic(request.getHeaderBgPic());
		}
		if (normalizedContentNode != null
				&& !normalizedContentNode.isNull()
				&& isTruthyJsonNode(normalizedContentNode)) {
			try {
				entity.setContent(objectMapper.writeValueAsString(normalizedContentNode));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("更新表单模板失败");
			}
		}
		if (StringUtils.hasText(request.getStatus())) {
			int p;
			try {
				p = Integer.parseInt(request.getStatus().trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException("status 无效");
			}
			if (p != 0) {
				entity.setStatus(p);
			}
		}
		if (StringUtils.hasText(request.getTemType())) {
			entity.setTemType(request.getTemType().trim());
		}
		if (normalizedKeyIndex != null
				&& !normalizedKeyIndex.isNull()
				&& isTruthyJsonNode(normalizedKeyIndex)) {
			try {
				entity.setKeyIndex(objectMapper.writeValueAsString(normalizedKeyIndex));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("更新表单模板失败");
			}
		}
		if (StringUtils.hasText(request.getFormStyle())) {
			entity.setFormStyle(request.getFormStyle());
		}
		if (request.getHeaderLinkTitle() != null) {
			entity.setHeaderLinkTitle(request.getHeaderLinkTitle());
		}
		if (request.getHeaderTitle() != null) {
			entity.setHeaderTitle(request.getHeaderTitle());
		}
		if (request.getBottomTitle() != null) {
			entity.setBottomTitle(request.getBottomTitle());
		}
	}

	private static boolean isTruthyJsonNode(JsonNode n) {
		if (n == null || n.isNull()) {
			return false;
		}
		if (n.isArray() && n.size() == 0) {
			return false;
		}
		return !(n.isObject() && !n.fieldNames().hasNext());
	}

	@Transactional(rollbackFor = Exception.class)
	public Object deleteData(String pathId) {
		if (isEarlyExitFormTemplatePathId(pathId)) {
			return Collections.emptyList();
		}
		long rowId;
		try {
			rowId = Long.parseLong(pathId);
		} catch (NumberFormatException ex) {
			throw new ResourceException("数据不存在");
		}
		if (rowId <= 0L) {
			throw new ResourceException("数据不存在");
		}
		int ts = (int) (System.currentTimeMillis() / 1000L);
		var wrapper = new LambdaUpdateWrapper<FormTemplate>()
				.set(FormTemplate::getStatus, 2)
				.set(FormTemplate::getUpdated, ts)
				.eq(FormTemplate::getId, rowId);
		int affected = formTemplateMapper.update(null, wrapper);
		if (affected == 0) {
			throw new ResourceException("数据不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Object restoreData(String pathId) {
		if (isEarlyExitFormTemplatePathId(pathId)) {
			return Collections.emptyList();
		}
		long rowId;
		try {
			rowId = Long.parseLong(pathId);
		} catch (NumberFormatException ex) {
			throw new ResourceException("数据不存在");
		}
		if (rowId <= 0L) {
			throw new ResourceException("数据不存在");
		}
		int ts = (int) (System.currentTimeMillis() / 1000L);
		var wrapper = new LambdaUpdateWrapper<FormTemplate>()
				.set(FormTemplate::getStatus, 1)
				.set(FormTemplate::getUpdated, ts)
				.eq(FormTemplate::getId, rowId);
		int affected = formTemplateMapper.update(null, wrapper);
		if (affected == 0) {
			throw new ResourceException("数据不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	private static boolean isEarlyExitFormTemplatePathId(String id) {
		if (id == null) {
			return true;
		}
		if (id.isEmpty()) {
			return true;
		}
		return "0".equals(id);
	}

}
