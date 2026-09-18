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
import cn.shopex.ecshopx.selfservice.domain.FormSetting;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormSettingCreateRequest;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormSettingOptionItem;
import cn.shopex.ecshopx.selfservice.mapper.FormSettingMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormSettingOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FormSettingCreateService {

	private static final Set<String> CHOICE_TYPES = Set.of("radio", "checkbox", "select");

	private final FormSettingMapper formSettingMapper;
	private final FormSettingOutsideMultiLangWriteService multiLangWriteService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;
	private final FormSettingApiRowAssembler rowAssembler;

	public FormSettingCreateService(
			FormSettingMapper formSettingMapper,
			FormSettingOutsideMultiLangWriteService multiLangWriteService,
			ObjectMapper objectMapper,
			MessageSource messageSource,
			FormSettingApiRowAssembler rowAssembler) {
		this.formSettingMapper = formSettingMapper;
		this.multiLangWriteService = multiLangWriteService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
		this.rowAssembler = rowAssembler;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createData(FormSettingCreateRequest request, long companyId, String requestLangTag) {
		if (!StringUtils.hasText(request.getFieldTitle())) {
			throw new BadRequestException("field_title必填");
		}
		if (!StringUtils.hasText(request.getFieldName())) {
			throw new BadRequestException("field_name必填");
		}
		if (!StringUtils.hasText(request.getFormElement())) {
			throw new BadRequestException("form_element必填");
		}

		String fieldTitle = request.getFieldTitle().trim();
		String fieldName = request.getFieldName().trim();
		String formElement = request.getFormElement().trim();
		if (!StringUtils.hasText(fieldTitle)) {
			throw new BadRequestException("field_title必填");
		}
		if (!StringUtils.hasText(fieldName)) {
			throw new BadRequestException("field_name必填");
		}
		if (!StringUtils.hasText(formElement)) {
			throw new BadRequestException("form_element必填");
		}

		Map<String, Object> serviceParams = new LinkedHashMap<>();
		serviceParams.put("field_title", fieldTitle);
		serviceParams.put("field_name", fieldName);
		serviceParams.put("form_element", formElement);
		serviceParams.put("options", request.getOptions());
		serviceParams.put("image_url", request.getImageUrl());
		serviceParams.put("pic_name", request.getPicName());
		serviceParams.put("company_id", companyId);

		LambdaQueryWrapper<FormSetting> w = new LambdaQueryWrapper<>();
		w.eq(FormSetting::getCompanyId, companyId).eq(FormSetting::getFieldName, fieldName).eq(FormSetting::getStatus, 1);
		formSettingMapper.selectCount(w);

		if (CHOICE_TYPES.contains(formElement)) {
			if (request.getOptions() == null) {
				throw new BadRequestException("options 不能为空");
			}
			List<FormSettingOptionItem> opts = new ArrayList<>(request.getOptions());
			opts.removeIf(item -> item == null || !StringUtils.hasText(item.getValue()));
			request.setOptions(opts);
		} else {
			request.setOptions(Collections.emptyList());
		}

		FormSetting entity = new FormSetting();
		entity.setCompanyId(companyId);
		entity.setFieldTitle(fieldTitle);
		entity.setFieldName(fieldName);
		entity.setFormElement(formElement);
		entity.setDistributorId(0L);
		if (StringUtils.hasText(request.getPicName())) {
			entity.setPicName(request.getPicName().trim());
		} else {
			entity.setPicName(null);
		}
		if (StringUtils.hasText(request.getImageUrl())) {
			entity.setImageUrl(request.getImageUrl().trim());
		} else {
			entity.setImageUrl("");
		}
		entity.setStatus(1);
		entity.setSort(1);
		entity.setIsRequired(Boolean.FALSE);

		if (CHOICE_TYPES.contains(formElement)) {
			List<FormSettingOptionItem> remaining = request.getOptions();
			if (remaining != null && !remaining.isEmpty()) {
				try {
					entity.setOptions(objectMapper.writeValueAsString(remaining));
				} catch (JsonProcessingException ex) {
					throw new ResourceException("创建表单配置失败");
				}
			} else {
				entity.setOptions(null);
			}
		} else {
			entity.setOptions(null);
		}

		int ts = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(ts);
		entity.setUpdated(ts);

		formSettingMapper.insert(entity);
		if (entity.getId() == null) {
			throw new ResourceException("创建表单配置失败");
		}

		Map<String, Object> rawForLang = new LinkedHashMap<>(serviceParams);
		rawForLang.put("field_title", fieldTitle);
		rawForLang.put("field_name", fieldName);
		rawForLang.put("form_element", formElement);
		rawForLang.put("options", request.getOptions());
		multiLangWriteService.addForNewFormSetting(entity.getId(), companyId, rawForLang, requestLangTag);

		return buildResponseMap(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateData(
			FormSettingCreateRequest request, long companyId, String requestLangTag, Locale locale) {
		if (request.getId() == null || !StringUtils.hasText(request.getId().trim())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_setting.tag_id_required", null, locale));
		}
		if (request.getFieldTitle() == null || !StringUtils.hasText(request.getFieldTitle().trim())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_setting.tag_name_required", null, locale));
		}
		if (request.getFieldName() == null || !StringUtils.hasText(request.getFieldName().trim())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_setting.tag_name_required", null, locale));
		}
		if (request.getFormElement() == null || !StringUtils.hasText(request.getFormElement().trim())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_setting.tag_name_required", null, locale));
		}

		long rowId;
		try {
			rowId = Long.parseLong(request.getId().trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_setting.tag_id_required", null, locale));
		}
		if (rowId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.form_setting.tag_id_required", null, locale));
		}

		String fieldTitleTrimmed = request.getFieldTitle().trim();
		String fieldNameTrimmed = request.getFieldName().trim();
		String formElementTrimmed = request.getFormElement().trim();

		LambdaQueryWrapper<FormSetting> dup = new LambdaQueryWrapper<>();
		dup.eq(FormSetting::getCompanyId, companyId)
				.eq(FormSetting::getFieldName, fieldNameTrimmed)
				.eq(FormSetting::getStatus, 1)
				.ne(FormSetting::getId, rowId);
		formSettingMapper.selectCount(dup);

		List<FormSettingOptionItem> optSource =
				request.getOptions() != null ? new ArrayList<>(request.getOptions()) : new ArrayList<>();
		if (CHOICE_TYPES.contains(formElementTrimmed)) {
			optSource.removeIf(item -> item == null || !StringUtils.hasText(item.getValue()));
			request.setOptions(optSource);
		} else {
			request.setOptions(Collections.emptyList());
		}

		LambdaQueryWrapper<FormSetting> one = new LambdaQueryWrapper<>();
		one.eq(FormSetting::getId, rowId).eq(FormSetting::getCompanyId, companyId);
		FormSetting entity = formSettingMapper.selectOne(one);
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.form_setting.no_update_data_found", null, locale));
		}

		if (StringUtils.hasText(fieldTitleTrimmed)) {
			entity.setFieldTitle(fieldTitleTrimmed);
		}
		if (StringUtils.hasText(fieldNameTrimmed)) {
			entity.setFieldName(fieldNameTrimmed);
		}
		if (StringUtils.hasText(formElementTrimmed)) {
			entity.setFormElement(formElementTrimmed);
		}
		if (StringUtils.hasText(request.getPicName())) {
			entity.setPicName(request.getPicName().trim());
		}
		if (StringUtils.hasText(request.getImageUrl())) {
			entity.setImageUrl(request.getImageUrl().trim());
		}
		if (CHOICE_TYPES.contains(formElementTrimmed)
				&& request.getOptions() != null
				&& !request.getOptions().isEmpty()) {
			try {
				entity.setOptions(objectMapper.writeValueAsString(request.getOptions()));
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("创建表单配置失败");
			}
		}
		entity.setUpdated((int) (System.currentTimeMillis() / 1000L));

		int rows = formSettingMapper.updateById(entity);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.form_setting.no_update_data_found", null, locale));
		}

		Map<String, Object> langBag = new LinkedHashMap<>();
		langBag.put("field_title", fieldTitleTrimmed);
		multiLangWriteService.updateLangDataForFormSetting(companyId, rowId, langBag, requestLangTag);

		return buildResponseMap(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Object restoreData(String pathId, long companyId) {
		if (isEarlyExitRestorePathId(pathId)) {
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
		LambdaUpdateWrapper<FormSetting> wrapper = new LambdaUpdateWrapper<>();
		wrapper
				.set(FormSetting::getStatus, 1)
				.eq(FormSetting::getId, rowId)
				.eq(FormSetting::getCompanyId, companyId);
		int affected = formSettingMapper.update(null, wrapper);
		if (affected == 0) {
			throw new ResourceException("数据不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Object deleteData(String pathId) {
		if (isEarlyExitRestorePathId(pathId)) {
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
		LambdaUpdateWrapper<FormSetting> wrapper = new LambdaUpdateWrapper<>();
		wrapper.set(FormSetting::getStatus, 2).eq(FormSetting::getId, rowId);
		int affected = formSettingMapper.update(null, wrapper);
		if (affected == 0) {
			throw new ResourceException("数据不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	private static boolean isEarlyExitRestorePathId(String id) {
		if (id == null) {
			return true;
		}
		if (id.isEmpty()) {
			return true;
		}
		return "0".equals(id);
	}

	private Map<String, Object> buildResponseMap(FormSetting entity) {
		return rowAssembler.toRow(entity);
	}
}
