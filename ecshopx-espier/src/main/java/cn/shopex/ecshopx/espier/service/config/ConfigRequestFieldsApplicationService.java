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

package cn.shopex.ecshopx.espier.service.config;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.espier.domain.ConfigRequestFields;
import cn.shopex.ecshopx.espier.mapper.ConfigRequestFieldsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ConfigRequestFieldsApplicationService {

	private static final DateTimeFormatter DESC_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final int CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC = 60;

	private static final Pattern CHIEF_APPLY_CN_MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

	private final ConfigRequestFieldsApplicationService self;
	private final ConfigRequestFieldsMapper configRequestFieldsMapper;
	private final RequestFieldDefaultConfigProvider requestFieldDefaultConfigProvider;
	private final ConfigRequestFieldsMultiLangReadService configRequestFieldsMultiLangReadService;
	private final ConfigRequestFieldsMultiLangWriteService configRequestFieldsMultiLangWriteService;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate companysRedisTemplate;

	public ConfigRequestFieldsApplicationService(
			@Lazy ConfigRequestFieldsApplicationService self,
			ConfigRequestFieldsMapper configRequestFieldsMapper,
			RequestFieldDefaultConfigProvider requestFieldDefaultConfigProvider,
			ConfigRequestFieldsMultiLangReadService configRequestFieldsMultiLangReadService,
			ConfigRequestFieldsMultiLangWriteService configRequestFieldsMultiLangWriteService,
			ObjectMapper objectMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.self = self;
		this.configRequestFieldsMapper = configRequestFieldsMapper;
		this.requestFieldDefaultConfigProvider = requestFieldDefaultConfigProvider;
		this.configRequestFieldsMultiLangReadService = configRequestFieldsMultiLangReadService;
		this.configRequestFieldsMultiLangWriteService = configRequestFieldsMultiLangWriteService;
		this.objectMapper = objectMapper;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void updateConfig(int companyId, int moduleType, Map<String, Object> data, int distributorId) {
		if (data == null) {
			return;
		}
		LinkedHashMap<String, String> fields = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : data.entrySet()) {
			String key = e.getKey();
			if (key == null || !ConfigRequestFieldsConstants.REQUEST_FIELD_SETTING_DEFAULTS.containsKey(key)) {
				continue;
			}
			Object value = e.getValue();
			String redisValue = value == null ? "" : String.valueOf(value);
			fields.put(key, redisValue);
		}
		if (fields.isEmpty()) {
			return;
		}
		String hashKey = "hash:ConfigRequestFieldsSetting_"
				+ moduleType
				+ "_"
				+ distributorId
				+ ":"
				+ sha1Hex(String.valueOf(companyId));
		companysRedisTemplate.opsForHash().putAll(hashKey, fields);
	}

	public Map<String, Object> getConfig(int companyId, int moduleType, int distributorId) {
		String hashKey = "hash:ConfigRequestFieldsSetting_"
				+ moduleType
				+ "_"
				+ distributorId
				+ ":"
				+ sha1Hex(String.valueOf(companyId));
		Map<Object, Object> raw = companysRedisTemplate.opsForHash().entries(hashKey);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		for (Map.Entry<Object, Object> entry : raw.entrySet()) {
			String key = String.valueOf(entry.getKey());
			data.put(key, entry.getValue());
		}
		for (Map.Entry<String, Integer> e : ConfigRequestFieldsConstants.REQUEST_FIELD_SETTING_DEFAULTS.entrySet()) {
			String k = e.getKey();
			if (!data.containsKey(k)) {
				data.put(k, e.getValue());
			}
		}
		String sk = ConfigRequestFieldsConstants.SETTING_SWITCH_FIRST_AUTH_FORCE_VALIDATION;
		int def = ConfigRequestFieldsConstants.REQUEST_FIELD_SETTING_DEFAULTS.get(sk);
		data.put(sk, intVal(data.get(sk), def));
		return data;
	}

	public void checkIsNeedInit(int companyId, int moduleType, int distributorId) {
		if (companyId <= 0) {
			return;
		}
		List<String> mustKeys =
				requestFieldDefaultConfigProvider.getMustStartAndRequiredFieldsFromConfig(companyId, moduleType);
		Map<String, Map<String, Object>> defaultFields =
				requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType);
		LinkedHashSet<String> allKeys = new LinkedHashSet<>();
		allKeys.addAll(mustKeys);
		allKeys.addAll(defaultFields.keySet());

		int expectedCount = mustKeys.size();
		for (Map.Entry<String, Map<String, Object>> e : defaultFields.entrySet()) {
			String field = e.getKey();
			if (defaultFieldConfigHasTruthyIsOpen(e.getValue()) && !mustKeys.contains(field)) {
				expectedCount++;
			}
		}

		long actualCount;
		if (allKeys.isEmpty()) {
			actualCount = 0L;
		} else {
			actualCount = configRequestFieldsMapper.selectCount(
					Wrappers.<ConfigRequestFields>lambdaQuery()
							.eq(ConfigRequestFields::getCompanyId, companyId)
							.eq(ConfigRequestFields::getModuleType, moduleType)
							.eq(ConfigRequestFields::getDistributorId, distributorId)
							.in(ConfigRequestFields::getKeyName, allKeys));
		}
		if (actualCount != expectedCount) {
			this.initForModule(companyId, moduleType, distributorId);
		}
	}

	private void initForModule(int companyId, int moduleType, int distributorId) {
		List<Map<String, Object>> forms = buildDefaultFieldForms(companyId, moduleType);
		for (Map<String, Object> formDatum : forms) {
			LinkedHashMap<String, Object> formMap = new LinkedHashMap<>(formDatum);
			formMap.put("distributor_id", distributorId);
			try {
				self.create(companyId, moduleType, formMap);
			} catch (ResourceException e) {
				String keyName = stringVal(formMap.get("key_name"));
				ConfigRequestFields existing = configRequestFieldsMapper.selectOne(
						Wrappers.<ConfigRequestFields>lambdaQuery()
								.eq(ConfigRequestFields::getCompanyId, companyId)
								.eq(ConfigRequestFields::getModuleType, moduleType)
								.eq(ConfigRequestFields::getKeyName, keyName)
								.eq(ConfigRequestFields::getDistributorId, distributorId));
				if (existing == null) {
					continue;
				}
				try {
					int id = existing.getId();
					self.updateInfo(companyId, id, formMap);
					boolean isOpen = intVal(formMap.get("is_open"), 0) != 0;
					boolean isRequired = intVal(formMap.get("is_required"), 0) != 0;
					boolean isEdit = intVal(formMap.get("is_edit"), 0) != 0;
					self.updateSwitch(
							companyId, id, ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_OPEN, isOpen, distributorId);
					self.updateSwitch(
							companyId,
							id,
							ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_REQUIRED,
							isRequired,
							distributorId);
					self.updateSwitch(
							companyId, id, ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_EDIT, isEdit, distributorId);
				} catch (Exception ex) {
					log.warn("initForModule update failed, key_name={}", keyName, ex);
				}
			} catch (Exception e) {
				log.warn("initForModule create failed, key_name={}", stringVal(formMap.get("key_name")), e);
			}
		}
	}

	public Map<String, Object> listPaginatedForAdmin(
			int companyId,
			int moduleType,
			int distributorId,
			int page,
			int pageSize,
			String acceptLanguageHeader) {
		checkIsNeedInit(companyId, moduleType, distributorId);
		LambdaQueryWrapper<ConfigRequestFields> base =
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getModuleType, moduleType)
						.eq(ConfigRequestFields::getDistributorId, distributorId)
						.orderByDesc(ConfigRequestFields::getId);
		long total = configRequestFieldsMapper.selectCount(base);
		Page<ConfigRequestFields> mpPage = new Page<>(page, pageSize, false);
		configRequestFieldsMapper.selectPage(mpPage, base);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ConfigRequestFields entity : mpPage.getRecords()) {
			rows.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rows, acceptLanguageHeader);
		List<Map<String, Object>> handled = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			try {
				handled.add(handleData(row));
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", handled);
		return out;
	}

	/**
	 * 后台「配置请求字段」分页列表：不按 distributor_id 过滤列表 COUNT/SELECT，初始化检查固定 distributorId=0。
	 */
	public Map<String, Object> listEspierAdminPaginated(
			int companyId,
			int moduleType,
			int idFilterOrSentinel,
			int page,
			int pageSize,
			String acceptLanguageHeader) {
		checkIsNeedInit(companyId, moduleType, 0);
		LambdaQueryWrapper<ConfigRequestFields> wrapper =
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getModuleType, moduleType);
		if (idFilterOrSentinel != -1) {
			wrapper.eq(ConfigRequestFields::getId, idFilterOrSentinel);
		}
		wrapper.orderByDesc(ConfigRequestFields::getId);
		long total = configRequestFieldsMapper.selectCount(wrapper);
		List<ConfigRequestFields> entities;
		if (pageSize > 0) {
			long offset = ((long) page - 1L) * (long) pageSize;
			wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
			entities = configRequestFieldsMapper.selectList(wrapper);
		} else {
			entities = configRequestFieldsMapper.selectList(wrapper);
		}
		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ConfigRequestFields entity : entities) {
			rowMaps.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rowMaps, acceptLanguageHeader);
		List<Map<String, Object>> handled = new ArrayList<>();
		for (Map<String, Object> row : rowMaps) {
			try {
				handled.add(handleData(row));
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", handled);
		return out;
	}

	private static boolean defaultFieldConfigHasTruthyIsOpen(Map<String, Object> def) {
		if (def == null || !def.containsKey("is_open")) {
			return false;
		}
		Object v = def.get("is_open");
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = stringVal(v);
		return !s.isEmpty() && !"0".equals(s);
	}

	private List<Map<String, Object>> buildDefaultFieldForms(int companyId, int moduleType) {
		Map<String, Map<String, Object>> defaultFields =
				new LinkedHashMap<>(
						requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType));
		List<String> mustKeys =
				requestFieldDefaultConfigProvider.getMustStartAndRequiredFieldsFromConfig(companyId, moduleType);
		for (String mk : mustKeys) {
			Map<String, Object> info = defaultFields.computeIfAbsent(mk, k -> new LinkedHashMap<>());
			info.put("is_open", true);
			info.put("is_required", true);
			if (!info.containsKey("name") || info.get("name") == null) {
				info.put("name", mk);
			}
			if (!info.containsKey("element_type") || stringVal(info.get("element_type")).isEmpty()) {
				info.put("element_type", "input");
			}
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map.Entry<String, Map<String, Object>> e : defaultFields.entrySet()) {
			out.add(buildSingleDefaultFieldForm(moduleType, e.getKey(), e.getValue()));
		}
		return out;
	}

	private Map<String, Object> buildSingleDefaultFieldForm(
			int moduleType, String keyName, Map<String, Object> info) {
		LinkedHashMap<String, Object> formDatum = new LinkedHashMap<>();
		formDatum.put("module_type", moduleType);
		formDatum.put("label", stringVal(info.get("name")));
		formDatum.put("key_name", keyName);
		formDatum.put("is_open", intVal(info.get("is_open"), 1));
		formDatum.put("is_required", intVal(info.get("is_required"), 1));
		formDatum.put("is_edit", 1);
		formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_TEXT);
		String prompt = stringVal(info.get("prompt"));
		formDatum.put("alert_required_message", prompt);
		if (prompt.isEmpty()) {
			formDatum.put("alert_required_message", String.format("请输入您的%s", formDatum.get("label")));
		}
		formDatum.put("range", new ArrayList<>());
		formDatum.put("radio_list", new ArrayList<>());
		String elementType = stringVal(info.get("element_type"));
		if (elementType.isEmpty()) {
			elementType = "input";
		}
		if (moduleType == ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO) {
			applyMemberDefaultFieldShape(keyName, elementType, info, formDatum);
		} else if (moduleType == ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO) {
			if ("mobile".equals(elementType)) {
				formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_MOBILE);
			}
		}
		return formDatum;
	}

	private void applyMemberDefaultFieldShape(
			String keyName, String elementType, Map<String, Object> info, LinkedHashMap<String, Object> formDatum) {
		switch (elementType) {
			case "select" -> {
				switch (keyName) {
					case "sex" -> {
						List<Object> radio = new ArrayList<>();
						radio.add(radioShapeRow(0, "未知", 0));
						radio.add(radioShapeRow(1, "男", 0));
						radio.add(radioShapeRow(2, "女", 0));
						formDatum.put("radio_list", radio);
						formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_RADIO);
					}
					case "birthday" -> {
						List<Object> rangeList = new ArrayList<>();
						LinkedHashMap<String, Object> one = new LinkedHashMap<>();
						one.put("start", null);
						one.put("end", null);
						rangeList.add(one);
						formDatum.put("range", rangeList);
						formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_DATE);
					}
					default -> {
						Object itemsObj = info.get("items");
						if (itemsObj instanceof Map<?, ?> itemsMap) {
							List<Object> radio = new ArrayList<>();
							for (Map.Entry<?, ?> en : itemsMap.entrySet()) {
								Object mapKey = en.getKey();
								LinkedHashMap<String, Object> row = new LinkedHashMap<>();
								row.put("value", mapKey == null ? "" : mapKey);
								row.put("label", stringVal(en.getValue()));
								row.put("is_checked", 0);
								radio.add(row);
							}
							formDatum.put("radio_list", radio);
							formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_RADIO);
						}
					}
				}
			}
			case "checkbox" -> {
				Object itemsObj = info.get("items");
				if (itemsObj instanceof Map<?, ?> itemsMap) {
					List<Object> radio = new ArrayList<>();
					for (Map.Entry<?, ?> en : itemsMap.entrySet()) {
						Object value = en.getKey() == null ? "" : en.getKey();
						Object item = en.getValue();
						if (item instanceof Map<?, ?> im) {
							String label = stringVal(im.get("name"));
							int checked = intVal(im.get("ischecked"), 0);
							LinkedHashMap<String, Object> row = new LinkedHashMap<>();
							row.put("value", value);
							row.put("label", label);
							row.put("is_checked", checked);
							radio.add(row);
						}
					}
					formDatum.put("radio_list", radio);
					formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_CHECKBOX);
				}
			}
			case "mobile" -> formDatum.put("field_type", ConfigRequestFieldsConstants.FIELD_TYPE_MOBILE);
			default -> {
			}
		}
	}

	public Map<String, Object> create(int companyId, int moduleType, Map<String, Object> formData) {
		String label = stringVal(formData.get("label"));
		String keyName = stringVal(formData.get("key_name"));
		int distributorId = intVal(formData.get("distributor_id"), 0);

		boolean isPreset = false;
		Map<String, Map<String, Object>> defaultFields =
				requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType);

		if (keyName.isEmpty()) {
			for (Map.Entry<String, Map<String, Object>> e : defaultFields.entrySet()) {
				String fieldKeyName = e.getKey();
				Map<String, Object> info = e.getValue();
				String fieldLabel = stringVal(info.get("name"));
				if (label.equals(fieldLabel)) {
					keyName = fieldKeyName;
					isPreset = true;
					break;
				}
			}
		} else {
			Map<String, Object> preset = defaultFields.get(keyName);
			if (preset != null && preset.containsKey("name")) {
				label = stringVal(preset.get("name"));
				isPreset = true;
			}
		}

		if (isPreset) {
			checkKeyExist(companyId, moduleType, keyName, 0, distributorId);
		}

		int fieldType = intVal(formData.get("field_type"), ConfigRequestFieldsConstants.FIELD_TYPE_TEXT);

		LinkedHashMap<String, Object> createData = new LinkedHashMap<>();
		createData.put("company_id", companyId);
		createData.put("distributor_id", distributorId);
		createData.put("module_type", moduleType);
		createData.put("label", label);
		createData.put("key_name", keyName);
		createData.put("is_preset", isPreset ? 1 : 0);
		createData.put("is_open", int01(formData.get("is_open")));
		createData.put("is_required", int01(formData.get("is_required")));
		createData.put("is_edit", int01(formData.get("is_edit")));
		createData.put("field_type", fieldType);
		createData.put("validate_condition", "");
		createData.put("alert_required_message", stringVal(formData.get("alert_required_message")));
		createData.put("alert_validate_message", stringVal(formData.get("alert_validate_message")));

		try {
			createData.put("validate_condition", writeJsonUnescaped(makeValidateCondition(fieldType, formData)));
		} catch (JsonProcessingException e) {
			throw new ResourceException("配置字段校验条件序列化失败");
		}

		checkLabelExist(companyId, moduleType, label, 0, distributorId);

		String rowKeyName = stringVal(createData.get("key_name"));
		List<String> mustKeys =
				requestFieldDefaultConfigProvider.getMustStartAndRequiredFieldsFromConfig(companyId, moduleType);
		if (mustKeys.contains(rowKeyName)) {
			createData.put("is_open", 1);
			createData.put("is_required", 1);
		}

		Map<String, Object> persisted = self.createInTransaction(companyId, moduleType, distributorId, isPreset, createData);

		String preventionKey = "prevention:ConfigRequestFieldsNewSetting_"
				+ moduleType
				+ "_"
				+ distributorId
				+ ":"
				+ sha1Hex(String.valueOf(companyId));
		companysRedisTemplate.delete(preventionKey);

		try {
			return handleData(persisted);
		} catch (JsonProcessingException e) {
			throw new ResourceException("配置字段数据解析失败");
		}
	}

	public void updateSwitch(int companyId, int id, int switchColumnKey, boolean isOpen, int distributorId) {
		ConfigRequestFields row = configRequestFieldsMapper.selectOne(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id)
						.eq(ConfigRequestFields::getDistributorId, distributorId));
		if (row == null) {
			throw new ResourceException("无法查询到该数据");
		}
		if (!ConfigRequestFieldsConstants.SWITCH_COLUMN_MAP.containsKey(switchColumnKey)) {
			throw new BadRequestException("操作失败！更新的字段有误！");
		}
		String infoKeyName = row.getKeyName() != null ? row.getKeyName() : "";
		int moduleType = row.getModuleType() != null ? row.getModuleType() : 0;
		// 关闭 is_open / is_required 时禁止动「必须开启且必填」字段
		if (!isOpen
				&& (switchColumnKey == ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_OPEN
						|| switchColumnKey == ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_REQUIRED)) {
			checkKeyNameIsMustStartAndRequired(companyId, moduleType, infoKeyName);
		}
		Map<String, Map<String, Object>> defaultFields =
				requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType);
		boolean presetFromDefaults = defaultFields.containsKey(infoKeyName);

		ConfigRequestFields patch = new ConfigRequestFields();
		patch.setUpdated((int) Instant.now().getEpochSecond());
		switch (switchColumnKey) {
			case ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_OPEN -> patch.setIsOpen(isOpen);
			case ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_REQUIRED -> patch.setIsRequired(isOpen);
			case ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_EDIT -> patch.setIsEdit(isOpen);
			case ConfigRequestFieldsConstants.SWITCH_COLUMN_IS_PRESET -> patch.setIsPreset(isOpen);
			default -> throw new BadRequestException("操作失败！更新的字段有误！");
		}
		patch.setIsPreset(presetFromDefaults);

		configRequestFieldsMapper.update(
				patch,
				Wrappers.<ConfigRequestFields>lambdaUpdate()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id));

		String preventionKey = "prevention:ConfigRequestFieldsNewSetting_"
				+ moduleType
				+ "_"
				+ distributorId
				+ ":"
				+ sha1Hex(String.valueOf(companyId));
		companysRedisTemplate.delete(preventionKey);
	}

	public Map<String, Object> updateInfo(int companyId, int id, Map<String, Object> formData) {
		int distributorId = intVal(formData.get("distributor_id"), 0);

		ConfigRequestFields row = configRequestFieldsMapper.selectOne(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id)
						.eq(ConfigRequestFields::getDistributorId, distributorId));
		if (row == null) {
			throw new ResourceException("操作失败！不存在该数据！");
		}

		int moduleType = intVal(row.getModuleType(), 0);
		String label = stringVal(formData.get("label"));
		int rowFieldTypeDefault = row.getFieldType() != null ? row.getFieldType() : 0;
		int fieldType = intVal(formData.get("field_type"), rowFieldTypeDefault);

		String vcJson;
		try {
			vcJson = writeJsonUnescaped(makeValidateCondition(fieldType, formData));
		} catch (JsonProcessingException e) {
			throw new ResourceException("配置字段校验条件序列化失败");
		}

		ConfigRequestFields patch = new ConfigRequestFields();
		patch.setUpdated((int) Instant.now().getEpochSecond());
		patch.setLabel(label);
		patch.setFieldType(fieldType);
		patch.setValidateCondition(vcJson);
		patch.setAlertRequiredMessage(stringVal(formData.get("alert_required_message")));
		if (formData.containsKey("alert_validate_message") && formData.get("alert_validate_message") != null) {
			patch.setAlertValidateMessage(stringVal(formData.get("alert_validate_message")));
		}

		checkLabelExist(companyId, moduleType, label, id, distributorId);

		ConfigRequestFields freshRow = self.updateInfoInTransaction(companyId, id, distributorId, formData, patch, vcJson);

		Map<String, Object> persisted = entityToRowMap(freshRow);
		try {
			return handleData(persisted);
		} catch (JsonProcessingException e) {
			throw new ResourceException("配置字段数据解析失败");
		}
	}

	/**
	 * 按商家、主键与分销商维度删除团长申请配置字段行；无匹配行时幂等；默认配置项中的 key 不可删。
	 */
	public void deleteForChiefApplyField(int companyId, int id, int distributorId) {
		ConfigRequestFields row = configRequestFieldsMapper.selectOne(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id)
						.eq(ConfigRequestFields::getDistributorId, distributorId));
		if (row == null) {
			return;
		}
		int moduleType = intVal(row.getModuleType(), 0);
		String keyName = row.getKeyName() != null ? row.getKeyName() : "";
		Map<String, Map<String, Object>> defaultFields =
				requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType);
		if (defaultFields.containsKey(keyName)) {
			throw new ResourceException(
					String.format("操作失败！该模块下【%s】是默认项，无法删除！", keyName));
		}
		configRequestFieldsMapper.delete(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id));
		int rowDistributorId = intVal(row.getDistributorId(), 0);
		String preventionKey = "prevention:ConfigRequestFieldsNewSetting_"
				+ moduleType
				+ "_"
				+ rowDistributorId
				+ ":"
				+ sha1Hex(String.valueOf(companyId));
		companysRedisTemplate.delete(preventionKey);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createInTransaction(
			int companyId,
			int moduleType,
			int distributorId,
			boolean isPreset,
			LinkedHashMap<String, Object> createData) {
		ConfigRequestFields entity = toEntityForInsert(createData);
		int now = (int) Instant.now().getEpochSecond();
		entity.setCreated(now);
		entity.setUpdated(now);
		configRequestFieldsMapper.insert(entity);

		Map<String, Object> result = entityToRowMap(entity);

		LinkedHashMap<String, Object> langSnapshot = new LinkedHashMap<>();
		langSnapshot.put("company_id", createData.get("company_id"));
		langSnapshot.put("label", createData.get("label"));
		langSnapshot.put("validate_condition", createData.get("validate_condition"));
		langSnapshot.put("alert_required_message", createData.get("alert_required_message"));
		configRequestFieldsMultiLangWriteService.syncAfterInsert(entity.getId(), langSnapshot);

		if (!isPreset) {
			int id = entity.getId();
			String newKey = DigestUtils.md5DigestAsHex(
					("config_request_field_" + id).getBytes(StandardCharsets.UTF_8));
			result.put("key_name", newKey);
			checkKeyExist(companyId, moduleType, newKey, id, distributorId);
			ConfigRequestFields patch = new ConfigRequestFields();
			patch.setId(id);
			patch.setKeyName(newKey);
			configRequestFieldsMapper.updateById(patch);
		}

		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public ConfigRequestFields updateInfoInTransaction(
			int companyId,
			int id,
			int distributorId,
			Map<String, Object> formData,
			ConfigRequestFields patch,
			String vcJson) {
		int rows = configRequestFieldsMapper.update(
				patch,
				Wrappers.<ConfigRequestFields>lambdaUpdate()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id));
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		ConfigRequestFields freshRow = configRequestFieldsMapper.selectOne(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getId, id)
						.eq(ConfigRequestFields::getDistributorId, distributorId));
		if (freshRow == null) {
			throw new ResourceException("未查询到更新数据");
		}

		LinkedHashMap<String, Object> langSnap = new LinkedHashMap<>();
		langSnap.put("company_id", companyId);
		langSnap.put("label", stringVal(freshRow.getLabel()));
		langSnap.put("validate_condition", vcJson);
		langSnap.put("alert_required_message", stringVal(freshRow.getAlertRequiredMessage()));
		configRequestFieldsMultiLangWriteService.syncAfterUpdate(freshRow.getId(), companyId, langSnap, "zh-CN");

		int moduleTypeForKey = intVal(freshRow.getModuleType(), 0);
		int distributorIdForKey = intVal(freshRow.getDistributorId(), 0);
		String preventionKey = "prevention:ConfigRequestFieldsNewSetting_"
				+ moduleTypeForKey
				+ "_"
				+ distributorIdForKey
				+ ":"
				+ sha1Hex(String.valueOf(companyId));
		companysRedisTemplate.delete(preventionKey);

		return freshRow;
	}

	private List<Object> makeValidateCondition(int fieldType, Map<String, Object> formData) {
		switch (fieldType) {
			case ConfigRequestFieldsConstants.FIELD_TYPE_NUMBER:
			case ConfigRequestFieldsConstants.FIELD_TYPE_DATE:
				return makeRangeValidateCondition(formData.get("range"));
			case ConfigRequestFieldsConstants.FIELD_TYPE_RADIO:
			case ConfigRequestFieldsConstants.FIELD_TYPE_CHECKBOX:
				return makeRadioValidateCondition(formData.get("radio_list"));
			default:
				return new ArrayList<>();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> makeRangeValidateCondition(Object rawRange) {
		if (rawRange == null) {
			return new ArrayList<>();
		}
		if (rawRange instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object itemObj : list) {
				Map<String, Object> item = itemObj instanceof Map<?, ?> m
						? new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m)
						: new LinkedHashMap<>();
				LinkedHashMap<String, Object> rangeRow = new LinkedHashMap<>();
				rangeRow.put("value", sprintfSs(item.get("start"), item.get("end")));
				rangeRow.put("label", "取值范围");
				rangeRow.put("is_checked", 0);
				out.add(rangeRow);
			}
			return out;
		}
		if (rawRange instanceof Map<?, ?> rangeMapRaw) {
			Map<String, Object> rangeMap = (Map<String, Object>) (Map<?, ?>) rangeMapRaw;
			boolean hasStart = rangeMap.containsKey("start");
			boolean hasEnd = rangeMap.containsKey("end");
			if (!hasStart && !hasEnd) {
				List<Object> out = new ArrayList<>();
				for (Object itemObj : rangeMap.values()) {
					Map<String, Object> item = itemObj instanceof Map<?, ?> m
							? new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m)
							: new LinkedHashMap<>();
					LinkedHashMap<String, Object> rangeRow = new LinkedHashMap<>();
					rangeRow.put("value", sprintfSs(item.get("start"), item.get("end")));
					rangeRow.put("label", "取值范围");
					rangeRow.put("is_checked", 0);
					out.add(rangeRow);
				}
				return out;
			}
			LinkedHashMap<String, Object> singleRange = new LinkedHashMap<>();
			singleRange.put("value", sprintfSs(rangeMap.get("start"), rangeMap.get("end")));
			singleRange.put("label", "取值范围");
			singleRange.put("is_checked", 0);
			return new ArrayList<>(Collections.singletonList(singleRange));
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	private List<Object> makeRadioValidateCondition(Object raw) {
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("操作失败！验证的数据格式有误！");
		}
		List<Object> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			Object el = list.get(i);
			if (!(el instanceof Map<?, ?> m)) {
				throw new BadRequestException("操作失败！验证的数据格式有误！");
			}
			Map<String, Object> item = new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m);
			// Each radio option must include label and is_checked; absent key or null value is invalid, empty string is valid.
			if (item.get("label") == null || item.get("is_checked") == null) {
				throw new BadRequestException("操作失败！验证的数据格式有误！");
			}
			item.put("value", i);
			result.add(item);
		}
		return result;
	}

	private static String sprintfSs(Object start, Object end) {
		String a = start == null ? "" : String.valueOf(start);
		String b = end == null ? "" : String.valueOf(end);
		return a + "," + b;
	}

	private static LinkedHashMap<String, Object> radioShapeRow(Object value, String label, int isChecked) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("value", value);
		m.put("label", label);
		m.put("is_checked", isChecked);
		return m;
	}

	/**
	 * 若 key_name 在配置「必须开启且必填」列表中，则不允许将开启/必填开关关断。
	 */
	private void checkKeyNameIsMustStartAndRequired(int companyId, int moduleType, String keyName) {
		List<String> must =
				requestFieldDefaultConfigProvider.getMustStartAndRequiredFieldsFromConfig(companyId, moduleType);
		if (must.contains(keyName)) {
			throw new ResourceException(
					String.format("操作失败！该模块下【%s】必须开启且必须是必填！", keyName));
		}
	}

	private void checkKeyExist(int companyId, int moduleType, String key, int neqId, int distributorId) {
		long c = configRequestFieldsMapper.selectCount(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getModuleType, moduleType)
						.eq(ConfigRequestFields::getKeyName, key)
						.eq(ConfigRequestFields::getDistributorId, distributorId)
						.ne(neqId > 0, ConfigRequestFields::getId, neqId));
		if (c > 0) {
			throw new ResourceException(String.format("操作失败！该模块下【%s】已存在！", key));
		}
	}

	private void checkLabelExist(int companyId, int moduleType, String label, int neqId, int distributorId) {
		long c = configRequestFieldsMapper.selectCount(
				Wrappers.<ConfigRequestFields>lambdaQuery()
						.eq(ConfigRequestFields::getCompanyId, companyId)
						.eq(ConfigRequestFields::getModuleType, moduleType)
						.eq(ConfigRequestFields::getLabel, label)
						.eq(ConfigRequestFields::getDistributorId, distributorId)
						.ne(neqId > 0, ConfigRequestFields::getId, neqId));
		if (c > 0) {
			throw new ResourceException(String.format("操作失败！该模块下【%s】已存在！", label));
		}
	}

	private String writeJsonUnescaped(Object value) throws JsonProcessingException {
		ObjectMapper utf = objectMapper.copy();
		utf.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
		return utf.writeValueAsString(value);
	}

	private ConfigRequestFields toEntityForInsert(Map<String, Object> createData) {
		ConfigRequestFields e = new ConfigRequestFields();
		e.setCompanyId((Integer) createData.get("company_id"));
		e.setDistributorId((Integer) createData.get("distributor_id"));
		e.setModuleType((Integer) createData.get("module_type"));
		e.setLabel(stringVal(createData.get("label")));
		e.setKeyName(stringVal(createData.get("key_name")));
		e.setIsPreset(int01(createData.get("is_preset")) == 1);
		e.setIsOpen(int01(createData.get("is_open")) == 1);
		e.setIsRequired(int01(createData.get("is_required")) == 1);
		e.setIsEdit(int01(createData.get("is_edit")) == 1);
		e.setFieldType((Integer) createData.get("field_type"));
		e.setValidateCondition(stringVal(createData.get("validate_condition")));
		e.setAlertRequiredMessage(stringVal(createData.get("alert_required_message")));
		e.setAlertValidateMessage(stringVal(createData.get("alert_validate_message")));
		return e;
	}

	private Map<String, Object> entityToRowMap(ConfigRequestFields e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("module_type", e.getModuleType());
		m.put("label", e.getLabel());
		m.put("key_name", e.getKeyName());
		m.put("is_preset", e.getIsPreset() != null && e.getIsPreset() ? 1 : 0);
		m.put("is_open", e.getIsOpen() != null && e.getIsOpen() ? 1 : 0);
		m.put("is_required", e.getIsRequired() != null && e.getIsRequired() ? 1 : 0);
		m.put("is_edit", e.getIsEdit() != null && e.getIsEdit() ? 1 : 0);
		m.put("field_type", e.getFieldType());
		m.put("validate_condition", e.getValidateCondition() != null ? e.getValidateCondition() : "");
		m.put("alert_required_message", e.getAlertRequiredMessage());
		m.put("alert_validate_message", e.getAlertValidateMessage());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private Map<String, Object> handleData(Map<String, Object> data) throws JsonProcessingException {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(data);

		Object ft = out.get("field_type");
		if (ft != null) {
			int fti = intVal(ft, 0);
			out.put("field_type_desc", ConfigRequestFieldsConstants.FIELD_TYPE_MAP.getOrDefault(fti, ""));
		}

		Object vcRaw = out.get("validate_condition");
		if (vcRaw instanceof String s) {
			if (s.isBlank()) {
				out.put("validate_condition", new ArrayList<>());
			} else {
				out.put("validate_condition", objectMapper.readValue(s, new TypeReference<List<Object>>() {}));
			}
		}

		makeFieldTypeContent(out);

		Object cr = out.get("created");
		if (cr != null) {
			out.put("created_desc", formatEpochSecond(cr));
		}
		Object up = out.get("updated");
		if (up != null) {
			out.put("updated_desc", formatEpochSecond(up));
		}

		out.put("is_must_start_required", 0);
		out.put("is_default", 0);

		int companyId = intVal(out.get("company_id"), 0);
		int moduleType = intVal(out.get("module_type"), 0);
		if (out.containsKey("module_type")) {
			out.put("module_type_desc", ConfigRequestFieldsConstants.MODULE_TYPE_MAP.getOrDefault(moduleType, ""));
			String keyName = stringVal(out.get("key_name"));
			List<String> must =
					requestFieldDefaultConfigProvider.getMustStartAndRequiredFieldsFromConfig(companyId, moduleType);
			if (must.contains(keyName)) {
				out.put("is_must_start_required", 1);
			}
			Map<String, Map<String, Object>> defs =
					requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType);
			if (defs.containsKey(keyName)) {
				out.put("is_default", 1);
			}
		}

		for (String field : List.of("field_type", "is_open", "is_required", "is_edit", "is_preset")) {
			if (out.containsKey(field)) {
				out.put(field, intVal(out.get(field), 0));
			}
		}

		if (out.get("company_id") != null
				&& out.get("module_type") != null
				&& out.get("label") != null
				&& out.get("key_name") != null) {
			Map<String, Map<String, Object>> transform =
					requestFieldDefaultConfigProvider.getDefaultFieldsFromConfig(companyId, moduleType);
			String kn = stringVal(out.get("key_name"));
			Map<String, Object> def = transform.get(kn);
			if (def != null && def.get("name") != null) {
				out.put("label", stringVal(def.get("name")));
			}
		}

		return out;
	}

	private String formatEpochSecond(Object raw) {
		int sec = intVal(raw, 0);
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneId.systemDefault());
		return ldt.format(DESC_FMT);
	}

	@SuppressWarnings("unchecked")
	private void makeFieldTypeContent(Map<String, Object> result) {
		if (!result.containsKey("range")) {
			// Empty range must serialize as a JSON array []; an empty Map would serialize as {}.
			result.put("range", new ArrayList<>());
		}
		if (!result.containsKey("radio_list")) {
			result.put("radio_list", new ArrayList<>());
		}
		if (!result.containsKey("validate_condition")) {
			return;
		}
		Object vcObj = result.get("validate_condition");
		Object validateCondition;
		if (vcObj instanceof String s) {
			try {
				validateCondition = objectMapper.readValue(s, new TypeReference<List<Object>>() {});
			} catch (JsonProcessingException e) {
				validateCondition = List.of();
			}
		} else {
			validateCondition = vcObj;
		}
		if (validateCondition == null) {
			return;
		}

		int fieldType = intVal(result.get("field_type"), 0);
		switch (fieldType) {
			case ConfigRequestFieldsConstants.FIELD_TYPE_NUMBER:
			case ConfigRequestFieldsConstants.FIELD_TYPE_DATE:
				if (validateCondition instanceof List<?> vcList) {
					for (Object itemObj : vcList) {
						if (!(itemObj instanceof Map<?, ?> im)) {
							continue;
						}
						Map<String, Object> item = (Map<String, Object>) (Map<?, ?>) im;
						String value = stringVal(item.get("value"));
						String[] valueArray = value.split(",", -1);
						String start = valueArray.length > 0 ? valueArray[0] : "";
						String end = valueArray.length > 1 ? valueArray[1] : "";
						LinkedHashMap<String, Object> rangeEntry = new LinkedHashMap<>();
						rangeEntry.put("start", isNumericString(start) ? start : null);
						rangeEntry.put("end", isNumericString(end) ? end : null);
						result.put("range", rangeEntry);
					}
				}
				break;
			case ConfigRequestFieldsConstants.FIELD_TYPE_RADIO:
			case ConfigRequestFieldsConstants.FIELD_TYPE_CHECKBOX:
				if (validateCondition instanceof List<?> vcList) {
					List<Map<String, Object>> radioList = new ArrayList<>();
					for (int key = 0; key < vcList.size(); key++) {
						Object itemObj = vcList.get(key);
						if (!(itemObj instanceof Map<?, ?> im)) {
							continue;
						}
						Map<String, Object> item = (Map<String, Object>) (Map<?, ?>) im;
						Object v = item.get("value");
						String valueStr = v != null ? String.valueOf(v) : String.valueOf(key);
						LinkedHashMap<String, Object> radioRow = new LinkedHashMap<>();
						radioRow.put("key", stringVal(item.get("key")));
						radioRow.put("value", valueStr);
						radioRow.put("label", stringVal(item.get("label")));
						radioRow.put("is_checked", intVal(item.get("is_checked"), 0));
						radioList.add(radioRow);
					}
					result.put("radio_list", radioList);
				}
				break;
			default:
				break;
		}
	}

	private static boolean isNumericString(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static int intVal(Object v, int defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int int01(Object v) {
		return intVal(v, 0);
	}

	/**
	 * 团长申请：加载并处理全部动态字段定义（含多语言），与列表接口数据结构一致。
	 */
	public List<Map<String, Object>> listAllChiefApplyFieldsHandled(
			int companyId, int distributorId, String acceptLanguageHeader) {
		checkIsNeedInit(companyId, ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO, distributorId);
		List<ConfigRequestFields> entities =
				configRequestFieldsMapper.selectList(
						Wrappers.<ConfigRequestFields>lambdaQuery()
								.eq(ConfigRequestFields::getCompanyId, companyId)
								.eq(ConfigRequestFields::getModuleType, ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO)
								.eq(ConfigRequestFields::getDistributorId, distributorId)
								.orderByDesc(ConfigRequestFields::getId));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ConfigRequestFields entity : entities) {
			rows.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rows, acceptLanguageHeader);
		List<Map<String, Object>> handled = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			try {
				handled.add(handleData(row));
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
		}
		return handled;
	}

	/**
	 * 团长申请字段 setting 形态列表（仅开启项），带与读侧 prevention 键一致的多语言段缓存。
	 */
	public List<Map<String, Object>> getChiefApplyFieldsSettingList(
			long companyId, int distributorId, String acceptLanguageHeader) {
		if (CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC <= 0) {
			throw new ResourceException("操作失败！缓存时间必须大于0秒");
		}
		String langSegment = normalizeLangForCacheKey(acceptLanguageHeader);
		int moduleType = ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO;
		String preventionKey = "prevention:ConfigRequestFieldsNewSetting_"
				+ moduleType
				+ "_"
				+ distributorId
				+ "_"
				+ langSegment
				+ ":"
				+ sha1Hex(String.valueOf(companyId));

		List<Map<String, Object>> cached = parsePreventionSettingListPayload(companysRedisTemplate.opsForValue().get(preventionKey));
		if (cached != null) {
			return cached;
		}

		String lockKey = preventionKey + ":lock";
		Boolean locked = companysRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(30));
		if (Boolean.TRUE.equals(locked)) {
			try {
				List<Map<String, Object>> inside = parsePreventionSettingListPayload(companysRedisTemplate.opsForValue().get(preventionKey));
				if (inside != null) {
					return inside;
				}
				List<Map<String, Object>> built =
						buildChiefApplyFieldsSettingListCore(companyId, distributorId, acceptLanguageHeader);
				LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
				payload.put("expire_time", Instant.now().getEpochSecond() + CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC);
				payload.put("data", built);
				try {
					companysRedisTemplate
							.opsForValue()
							.set(
									preventionKey,
									objectMapper.writeValueAsString(payload),
									Duration.ofSeconds(CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC));
				} catch (JsonProcessingException e) {
					throw new ResourceException("配置字段数据解析失败");
				}
				return built;
			} finally {
				companysRedisTemplate.delete(lockKey);
			}
		}

		for (int i = 0; i < 30; i++) {
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			List<Map<String, Object>> retry = parsePreventionSettingListPayload(companysRedisTemplate.opsForValue().get(preventionKey));
			if (retry != null) {
				return retry;
			}
		}
		// 未持锁时仅依赖缓存；轮询仍无数据则不回源，避免与持锁方重建并发冲突
		return Collections.emptyList();
	}

	/**
	 * 管理端会员注册项：按 key_name 索引的 setting 映射（仅开启行），带 prevention 缓存。
	 */
	/**
	 * H5 会员资料编辑：按 key_name 索引的完整字段定义（多语言、handleData 后结构），供归一化与 lazy 校验。
	 */
	public LinkedHashMap<String, Map<String, Object>> loadH5MemberInfoEditableFieldsByKeyName(
			long companyId, String acceptLanguage) {
		if (companyId <= 0L) {
			return new LinkedHashMap<>();
		}
		int cid = (int) companyId;
		checkIsNeedInit(cid, ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO, 0);
		List<ConfigRequestFields> entities =
				configRequestFieldsMapper.selectList(
						Wrappers.<ConfigRequestFields>lambdaQuery()
								.eq(ConfigRequestFields::getCompanyId, cid)
								.eq(ConfigRequestFields::getModuleType, ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO)
								.eq(ConfigRequestFields::getDistributorId, 0)
								.eq(ConfigRequestFields::getIsOpen, true)
								.orderByDesc(ConfigRequestFields::getId));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ConfigRequestFields entity : entities) {
			rows.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rows, acceptLanguage);
		LinkedHashMap<String, Map<String, Object>> byKeyName = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			Map<String, Object> handled;
			try {
				handled = handleData(row);
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
			String keyName = stringVal(handled.get("key_name"));
			if (!keyName.isEmpty()) {
				byKeyName.put(keyName, handled);
			}
		}
		return byKeyName;
	}

	public LinkedHashMap<String, Map<String, Object>> loadMemberRegisterSettingByKeyNameForAdmin(
			int companyId, String acceptLanguageHeader) {
		checkIsNeedInit(companyId, ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO, 0);
		if (CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC <= 0) {
			throw new ResourceException("操作失败！缓存时间必须大于0秒");
		}
		String langSegment = normalizeLangForCacheKey(acceptLanguageHeader);
		int moduleType = ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		String preventionKey = "prevention:ConfigRequestFieldsNewSetting_"
				+ moduleType
				+ "_0_"
				+ langSegment
				+ ":"
				+ sha1Hex(String.valueOf(companyId));

		LinkedHashMap<String, Map<String, Object>> cached =
				parsePreventionSettingMapPayload(companysRedisTemplate.opsForValue().get(preventionKey));
		if (cached != null) {
			return cached;
		}

		String lockKey = preventionKey + ":lock";
		Boolean locked = companysRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(30));
		if (Boolean.TRUE.equals(locked)) {
			try {
				LinkedHashMap<String, Map<String, Object>> inside =
						parsePreventionSettingMapPayload(companysRedisTemplate.opsForValue().get(preventionKey));
				if (inside != null) {
					return inside;
				}
				LinkedHashMap<String, Map<String, Object>> built =
						buildMemberRegisterSettingByKeyNameCore(companyId, acceptLanguageHeader);
				LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
				payload.put("expire_time", Instant.now().getEpochSecond() + CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC);
				payload.put("data", built);
				try {
					companysRedisTemplate
							.opsForValue()
							.set(
									preventionKey,
									objectMapper.writeValueAsString(payload),
									Duration.ofSeconds(CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC));
				} catch (JsonProcessingException e) {
					throw new ResourceException("配置字段数据解析失败");
				}
				return built;
			} finally {
				companysRedisTemplate.delete(lockKey);
			}
		}

		for (int i = 0; i < 30; i++) {
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			LinkedHashMap<String, Map<String, Object>> retry =
					parsePreventionSettingMapPayload(companysRedisTemplate.opsForValue().get(preventionKey));
			if (retry != null) {
				return retry;
			}
		}
		return new LinkedHashMap<>();
	}

	public LinkedHashMap<String, Map<String, Object>> loadWxappMemberRegSettingByKeyName(
			int companyId, String acceptLanguageHeader) {
		checkIsNeedInit(companyId, ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO, 0);
		if (CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC <= 0) {
			throw new ResourceException("操作失败！缓存时间必须大于0秒");
		}
		String langSegment = normalizeLangForCacheKey(acceptLanguageHeader);
		int moduleType = ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO;
		String preventionKey = "prevention:ConfigRequestFieldsWxappMemberRegSetting_"
				+ moduleType
				+ "_0_"
				+ langSegment
				+ ":"
				+ sha1Hex(String.valueOf(companyId));

		LinkedHashMap<String, Map<String, Object>> cached =
				parsePreventionSettingMapPayload(companysRedisTemplate.opsForValue().get(preventionKey));
		if (cached != null) {
			return cached;
		}

		String lockKey = preventionKey + ":lock";
		Boolean locked = companysRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(30));
		if (Boolean.TRUE.equals(locked)) {
			try {
				LinkedHashMap<String, Map<String, Object>> inside =
						parsePreventionSettingMapPayload(companysRedisTemplate.opsForValue().get(preventionKey));
				if (inside != null) {
					return inside;
				}
				LinkedHashMap<String, Map<String, Object>> built =
						buildWxappMemberRegSettingByKeyNameCore(companyId, acceptLanguageHeader);
				LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
				payload.put("expire_time", Instant.now().getEpochSecond() + CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC);
				payload.put("data", built);
				try {
					companysRedisTemplate
							.opsForValue()
							.set(
									preventionKey,
									objectMapper.writeValueAsString(payload),
									Duration.ofSeconds(CHIEF_APPLY_FIELDS_SETTING_CACHE_TTL_SEC));
				} catch (JsonProcessingException e) {
					throw new ResourceException("配置字段数据解析失败");
				}
				return built;
			} finally {
				companysRedisTemplate.delete(lockKey);
			}
		}

		for (int i = 0; i < 30; i++) {
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			LinkedHashMap<String, Map<String, Object>> retry =
					parsePreventionSettingMapPayload(companysRedisTemplate.opsForValue().get(preventionKey));
			if (retry != null) {
				return retry;
			}
		}
		return new LinkedHashMap<>();
	}

	private String normalizeLangForCacheKey(String acceptLanguageHeader) {
		String resolved;
		if (!StringUtils.hasText(acceptLanguageHeader)) {
			resolved = "zh-CN";
		} else {
			String first = acceptLanguageHeader.split(",")[0].trim();
			int semi = first.indexOf(';');
			if (semi >= 0) {
				first = first.substring(0, semi).trim();
			}
			resolved = StringUtils.hasText(first) ? first : "zh-CN";
		}
		return resolved.toLowerCase(Locale.ROOT).replace("-", "");
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parsePreventionSettingListPayload(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			Map<String, Object> wrapped = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			Object dataObj = wrapped.get("data");
			if (!(dataObj instanceof List<?> list)) {
				return null;
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					out.add(new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m));
				}
			}
			return out;
		} catch (JsonProcessingException e) {
			log.debug("prevention setting list parse failed", e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<String, Map<String, Object>> parsePreventionSettingMapPayload(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			Map<String, Object> wrapped = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			Object dataObj = wrapped.get("data");
			if (!(dataObj instanceof Map<?, ?> dataMap)) {
				return null;
			}
			LinkedHashMap<String, Map<String, Object>> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : dataMap.entrySet()) {
				if (en.getKey() == null) {
					continue;
				}
				String k = String.valueOf(en.getKey());
				Object v = en.getValue();
				if (!(v instanceof Map<?, ?> vm)) {
					continue;
				}
				out.put(k, new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) vm));
			}
			return out;
		} catch (JsonProcessingException e) {
			log.debug("prevention setting map parse failed", e);
			return null;
		}
	}

	private List<Map<String, Object>> buildChiefApplyFieldsSettingListCore(
			long companyId, int distributorId, String acceptLanguageHeader) {
		checkIsNeedInit((int) companyId, ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO, distributorId);
		List<ConfigRequestFields> entities =
				configRequestFieldsMapper.selectList(
						Wrappers.<ConfigRequestFields>lambdaQuery()
								.eq(ConfigRequestFields::getCompanyId, (int) companyId)
								.eq(ConfigRequestFields::getModuleType, ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO)
								.eq(ConfigRequestFields::getDistributorId, distributorId)
								.eq(ConfigRequestFields::getIsOpen, true)
								.orderByDesc(ConfigRequestFields::getId));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ConfigRequestFields entity : entities) {
			rows.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rows, acceptLanguageHeader);
		LinkedHashMap<String, Map<String, Object>> byKeyName = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			String kn = stringVal(row.get("key_name"));
			if (!kn.isEmpty()) {
				byKeyName.put(kn, row);
			}
		}
		List<Map<String, Object>> settings = new ArrayList<>();
		for (Map<String, Object> row : byKeyName.values()) {
			Map<String, Object> handled;
			try {
				handled = handleData(row);
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
			settings.add(toSettingFormatFromHandledRow(handled, false));
		}
		return settings;
	}

	private LinkedHashMap<String, Map<String, Object>> buildMemberRegisterSettingByKeyNameCore(
			int companyId, String acceptLanguageHeader) {
		List<ConfigRequestFields> entities =
				configRequestFieldsMapper.selectList(
						Wrappers.<ConfigRequestFields>lambdaQuery()
								.eq(ConfigRequestFields::getCompanyId, companyId)
								.eq(ConfigRequestFields::getModuleType, ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO)
								.eq(ConfigRequestFields::getDistributorId, 0)
								.eq(ConfigRequestFields::getIsOpen, true)
								.orderByDesc(ConfigRequestFields::getId));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ConfigRequestFields entity : entities) {
			rows.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rows, acceptLanguageHeader);
		LinkedHashMap<String, Map<String, Object>> byKeyName = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			String kn = stringVal(row.get("key_name"));
			if (!kn.isEmpty()) {
				byKeyName.put(kn, row);
			}
		}
		LinkedHashMap<String, Map<String, Object>> setting = new LinkedHashMap<>();
		for (Map<String, Object> row : byKeyName.values()) {
			Map<String, Object> handled;
			try {
				handled = handleData(row);
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
			String keyName = stringVal(handled.get("key_name"));
			if (keyName.isEmpty()) {
				continue;
			}
			setting.put(keyName, toMemberRegisterSettingEntry(handled));
		}
		return setting;
	}

	private LinkedHashMap<String, Map<String, Object>> buildWxappMemberRegSettingByKeyNameCore(
			int companyId, String acceptLanguageHeader) {
		List<ConfigRequestFields> entities =
				configRequestFieldsMapper.selectList(
						Wrappers.<ConfigRequestFields>lambdaQuery()
								.eq(ConfigRequestFields::getCompanyId, companyId)
								.eq(ConfigRequestFields::getModuleType, ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO)
								.eq(ConfigRequestFields::getDistributorId, 0)
								.eq(ConfigRequestFields::getIsOpen, true)
								.orderByDesc(ConfigRequestFields::getId));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (ConfigRequestFields entity : entities) {
			rows.add(entityToRowMap(entity));
		}
		configRequestFieldsMultiLangReadService.applyToRows(rows, acceptLanguageHeader);
		LinkedHashMap<String, Map<String, Object>> byKeyName = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			String kn = stringVal(row.get("key_name"));
			if (!kn.isEmpty()) {
				byKeyName.put(kn, row);
			}
		}
		LinkedHashMap<String, Map<String, Object>> setting = new LinkedHashMap<>();
		for (Map<String, Object> row : byKeyName.values()) {
			Map<String, Object> handled;
			try {
				handled = handleData(row);
			} catch (JsonProcessingException e) {
				throw new ResourceException("配置字段数据解析失败");
			}
			String keyName = stringVal(handled.get("key_name"));
			if (keyName.isEmpty()) {
				continue;
			}
			setting.put(keyName, toSettingFormatFromHandledRow(handled, true));
		}
		return setting;
	}

	private Map<String, Object> toMemberRegisterSettingEntry(Map<String, Object> handled) {
		int fieldType = intVal(handled.get("field_type"), 0);
		String elementType =
				ConfigRequestFieldsConstants.FIELD_TYPE_ELEMENT_MAP.getOrDefault(fieldType, "input");
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("name", stringVal(handled.get("label")));
		out.put("element_type", elementType);
		out.put("is_open", intVal(handled.get("is_open"), 0) != 0);
		out.put("is_required", intVal(handled.get("is_required"), 0) != 0);
		if ("checkbox".equals(elementType)) {
			out.put("items", radioListToCheckboxItems(handled.get("radio_list")));
		} else if ("select".equals(elementType)) {
			out.put("items", buildSelectItemsFromRadioList(handled.get("radio_list")));
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private Object buildSelectItemsFromRadioList(Object radioListRaw) {
		if (!(radioListRaw instanceof List<?> list) || list.isEmpty()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> parsed = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			parsed.add((Map<String, Object>) (Map<?, ?>) m);
		}
		if (parsed.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, String> select = new LinkedHashMap<>();
		for (Map<String, Object> opt : parsed) {
			select.put(String.valueOf(opt.get("value")), stringVal(opt.get("label")));
		}
		List<Integer> sortedKeys = new ArrayList<>(select.size());
		for (String k : select.keySet()) {
			try {
				sortedKeys.add(Integer.parseInt(k.trim()));
			} catch (NumberFormatException e) {
				return select;
			}
		}
		Collections.sort(sortedKeys);
		int n = select.size();
		if (sortedKeys.size() != n) {
			return select;
		}
		for (int i = 0; i < n; i++) {
			if (sortedKeys.get(i) != i) {
				return select;
			}
		}
		ArrayList<String> labels = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			labels.add(select.get(String.valueOf(i)));
		}
		return labels;
	}

	private Map<String, Object> toSettingFormatFromHandledRow(
			Map<String, Object> handled, boolean wxappMemberRegH5JsonShape) {
		int fieldType = intVal(handled.get("field_type"), 0);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("name", stringVal(handled.get("label")));
		out.put("key", stringVal(handled.get("key_name")));
		out.put("is_open", intVal(handled.get("is_open"), 0) != 0);
		out.put("is_required", intVal(handled.get("is_required"), 0) != 0);
		out.put("is_edit", intVal(handled.get("is_edit"), 0) != 0);
		out.put("is_default", intVal(handled.get("is_preset"), 0) != 0);
		out.put(
				"element_type",
				ConfigRequestFieldsConstants.FIELD_TYPE_ELEMENT_MAP.getOrDefault(fieldType, "input"));
		out.put("field_type", fieldType);
		out.put("required_message", stringVal(handled.get("alert_required_message")));
		out.put("validate_message", stringVal(handled.get("alert_validate_message")));

		switch (fieldType) {
			case ConfigRequestFieldsConstants.FIELD_TYPE_NUMBER:
			case ConfigRequestFieldsConstants.FIELD_TYPE_DATE:
				if (wxappMemberRegH5JsonShape) {
					out.put("range", rangeAsSingleObjectMapForSetting(handled.get("range")));
				} else {
					out.put("range", rangeAsObjectListForSetting(handled.get("range")));
				}
				out.put("select", new ArrayList<>());
				out.put("checkbox", new ArrayList<>());
				break;
			case ConfigRequestFieldsConstants.FIELD_TYPE_RADIO:
				out.put("range", new ArrayList<>());
				if (wxappMemberRegH5JsonShape) {
					out.put("select", buildSelectItemsFromRadioList(handled.get("radio_list")));
				} else {
					out.put("select", radioListToSelectMap(handled.get("radio_list")));
				}
				out.put("checkbox", new ArrayList<>());
				break;
			case ConfigRequestFieldsConstants.FIELD_TYPE_CHECKBOX:
				out.put("range", new ArrayList<>());
				out.put("select", new ArrayList<>());
				out.put("checkbox", radioListToCheckboxItems(handled.get("radio_list")));
				break;
			default:
				out.put("range", new ArrayList<>());
				out.put("select", new ArrayList<>());
				out.put("checkbox", new ArrayList<>());
				break;
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> rangeAsSingleObjectMapForSetting(Object rawRange) {
		if (rawRange == null) {
			return new LinkedHashMap<>();
		}
		if (rawRange instanceof Map<?, ?> m) {
			return new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m);
		}
		if (rawRange instanceof List<?> list) {
			for (int i = list.size() - 1; i >= 0; i--) {
				Object o = list.get(i);
				if (o instanceof Map<?, ?> m) {
					return new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m);
				}
			}
		}
		return new LinkedHashMap<>();
	}

	@SuppressWarnings("unchecked")
	private static List<Object> rangeAsObjectListForSetting(Object rawRange) {
		if (rawRange == null) {
			return new ArrayList<>();
		}
		if (rawRange instanceof List<?> list) {
			List<Object> copy = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					copy.add(new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m));
				} else {
					copy.add(o);
				}
			}
			return copy;
		}
		if (rawRange instanceof Map<?, ?> m) {
			return new ArrayList<>(List.of(new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) m)));
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, String> radioListToSelectMap(Object radioRaw) {
		LinkedHashMap<String, String> select = new LinkedHashMap<>();
		if (!(radioRaw instanceof List<?> list)) {
			return select;
		}
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Map<String, Object> opt = (Map<String, Object>) (Map<?, ?>) m;
			String valueKey = String.valueOf(opt.get("value"));
			select.put(valueKey, stringVal(opt.get("label")));
		}
		return select;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> radioListToCheckboxItems(Object radioRaw) {
		List<Map<String, Object>> checkbox = new ArrayList<>();
		if (!(radioRaw instanceof List<?> list)) {
			return checkbox;
		}
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Map<String, Object> opt = (Map<String, Object>) (Map<?, ?>) m;
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("name", stringVal(opt.get("label")));
			item.put("ischecked", intVal(opt.get("is_checked"), 0) != 0);
			checkbox.add(item);
		}
		return checkbox;
	}

	/**
	 * 单选：将描述或索引规范为配置中的选项值。
	 */
	public void transformChiefApplyValuesByDesc(List<Map<String, Object>> fieldDefs, Map<String, Object> params) {
		if (fieldDefs == null || params == null) {
			return;
		}
		for (Map<String, Object> field : fieldDefs) {
			int ft = intVal(field.get("field_type"), 0);
			if (ft != ConfigRequestFieldsConstants.FIELD_TYPE_RADIO) {
				continue;
			}
			String keyName = stringVal(field.get("key_name"));
			if (keyName.isEmpty() || !params.containsKey(keyName)) {
				continue;
			}
			Object rawVal = params.get(keyName);
			if (rawVal == null) {
				continue;
			}
			String submitted = String.valueOf(rawVal).trim();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> radioList = (List<Map<String, Object>>) field.get("radio_list");
			if (radioList == null || radioList.isEmpty()) {
				continue;
			}
			boolean matched = false;
			for (int i = 0; i < radioList.size(); i++) {
				Map<String, Object> opt = radioList.get(i);
				String optValue = stringVal(opt.get("value"));
				String optLabel = stringVal(opt.get("label"));
				String optKey = stringVal(opt.get("key"));
				if (submitted.equals(optValue) || submitted.equals(optLabel) || submitted.equals(optKey)) {
					params.put(keyName, optValue.isEmpty() ? String.valueOf(i) : optValue);
					matched = true;
					break;
				}
			}
			if (!matched) {
				try {
					int idx = Integer.parseInt(submitted);
					if (idx >= 0 && idx < radioList.size()) {
						Map<String, Object> opt = radioList.get(idx);
						params.put(keyName, stringVal(opt.get("value")));
					}
				} catch (NumberFormatException ignored) {
					/* keep submitted */
				}
			}
		}
	}

	/**
	 * lazy：仅校验请求中出现且配置为开启、可编辑的字段。
	 */
	public void validateChiefApplyLazy(List<Map<String, Object>> fieldDefs, Map<String, Object> params) {
		if (fieldDefs == null || params == null) {
			return;
		}
		for (Map<String, Object> field : fieldDefs) {
			String keyName = stringVal(field.get("key_name"));
			if (keyName.isEmpty() || !params.containsKey(keyName)) {
				continue;
			}
			if (intVal(field.get("is_open"), 0) == 0 || intVal(field.get("is_edit"), 0) == 0) {
				continue;
			}
			validateChiefApplySingleField(field, params.get(keyName));
		}
	}

	private void validateChiefApplySingleField(Map<String, Object> field, Object rawValue) {
		int ft = intVal(field.get("field_type"), 0);
		boolean required = intVal(field.get("is_required"), 0) != 0;
		String reqMsg = stringVal(field.get("alert_required_message"));
		if (reqMsg.isEmpty()) {
			reqMsg = "请填写该字段";
		}
		String valMsg = stringVal(field.get("alert_validate_message"));
		if (valMsg.isEmpty()) {
			valMsg = "格式不正确";
		}

		if (isEmptyChiefApplyValue(rawValue)) {
			if (required) {
				throw new ResourceException(reqMsg);
			}
			return;
		}

		switch (ft) {
			case ConfigRequestFieldsConstants.FIELD_TYPE_TEXT -> validateChiefApplyText(rawValue, valMsg);
			case ConfigRequestFieldsConstants.FIELD_TYPE_NUMBER -> validateChiefApplyNumber(field, rawValue, valMsg);
			case ConfigRequestFieldsConstants.FIELD_TYPE_DATE -> validateChiefApplyDate(rawValue, valMsg);
			case ConfigRequestFieldsConstants.FIELD_TYPE_RADIO -> validateChiefApplyRadio(field, rawValue, valMsg);
			case ConfigRequestFieldsConstants.FIELD_TYPE_CHECKBOX -> validateChiefApplyCheckbox(field, rawValue, valMsg);
			case ConfigRequestFieldsConstants.FIELD_TYPE_MOBILE -> validateChiefApplyMobile(rawValue, valMsg);
			case ConfigRequestFieldsConstants.FIELD_TYPE_IMAGE -> validateChiefApplyImage(rawValue, valMsg);
			default -> {
			}
		}
	}

	private static boolean isEmptyChiefApplyValue(Object rawValue) {
		if (rawValue == null) {
			return true;
		}
		if (rawValue instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (rawValue instanceof Collection<?> c) {
			return c.isEmpty();
		}
		return false;
	}

	private static void validateChiefApplyText(Object rawValue, String valMsg) {
		String s = String.valueOf(rawValue).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(valMsg);
		}
	}

	private void validateChiefApplyNumber(Map<String, Object> field, Object rawValue, String valMsg) {
		double num;
		try {
			num = Double.parseDouble(String.valueOf(rawValue).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(valMsg);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> range = (Map<String, Object>) field.get("range");
		if (range == null) {
			return;
		}
		Object startObj = range.get("start");
		Object endObj = range.get("end");
		if (startObj != null && StringUtils.hasText(String.valueOf(startObj))) {
			try {
				double start = Double.parseDouble(String.valueOf(startObj).trim());
				if (num < start) {
					throw new ResourceException(valMsg);
				}
			} catch (NumberFormatException ignored) {
				/* skip malformed range bound */
			}
		}
		if (endObj != null && StringUtils.hasText(String.valueOf(endObj))) {
			try {
				double end = Double.parseDouble(String.valueOf(endObj).trim());
				if (num > end) {
					throw new ResourceException(valMsg);
				}
			} catch (NumberFormatException ignored) {
				/* skip malformed range bound */
			}
		}
	}

	private void validateChiefApplyDate(Object rawValue, String valMsg) {
		Long sec = DateExpressionParser.parseToEpochSecond(rawValue, ZoneId.systemDefault());
		if (sec == null) {
			throw new ResourceException(valMsg);
		}
	}

	@SuppressWarnings("unchecked")
	private void validateChiefApplyRadio(Map<String, Object> field, Object rawValue, String valMsg) {
		String submitted = String.valueOf(rawValue).trim();
		List<Map<String, Object>> radioList = (List<Map<String, Object>>) field.get("radio_list");
		if (radioList == null || radioList.isEmpty()) {
			return;
		}
		for (int i = 0; i < radioList.size(); i++) {
			Map<String, Object> opt = radioList.get(i);
			String optValue = stringVal(opt.get("value"));
			String optLabel = stringVal(opt.get("label"));
			if (submitted.equals(optValue)
					|| submitted.equals(optLabel)
					|| submitted.equals(String.valueOf(i))) {
				return;
			}
		}
		throw new ResourceException(valMsg);
	}

	@SuppressWarnings("unchecked")
	private void validateChiefApplyCheckbox(Map<String, Object> field, Object rawValue, String valMsg) {
		List<Map<String, Object>> radioList = (List<Map<String, Object>>) field.get("radio_list");
		if (radioList == null || radioList.isEmpty()) {
			return;
		}
		List<String> tokens = new ArrayList<>();
		if (rawValue instanceof List<?> list) {
			for (Object el : list) {
				tokens.add(String.valueOf(el).trim());
			}
		} else {
			String s = String.valueOf(rawValue).trim();
			if (StringUtils.hasText(s)) {
				for (String part : s.split(",")) {
					String t = part.trim();
					if (StringUtils.hasText(t)) {
						tokens.add(t);
					}
				}
			}
		}
		for (String token : tokens) {
			boolean ok = false;
			for (Map<String, Object> opt : radioList) {
				String optValue = stringVal(opt.get("value"));
				String optLabel = stringVal(opt.get("label"));
				if (token.equals(optValue) || token.equals(optLabel)) {
					ok = true;
					break;
				}
			}
			if (!ok) {
				throw new ResourceException(valMsg);
			}
		}
	}

	private static void validateChiefApplyMobile(Object rawValue, String valMsg) {
		String s = String.valueOf(rawValue).trim();
		if (!CHIEF_APPLY_CN_MOBILE.matcher(s).matches()) {
			throw new ResourceException(valMsg);
		}
	}

	private static void validateChiefApplyImage(Object rawValue, String valMsg) {
		String s = String.valueOf(rawValue).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(valMsg);
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException("内部摘要算法不可用");
		}
	}
}
