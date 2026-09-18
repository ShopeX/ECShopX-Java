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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Slider;
import cn.shopex.ecshopx.distribution.mapper.SliderMapper;
import cn.shopex.ecshopx.distribution.support.SliderColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class ShopScreenSliderSaveService {

	private final SliderMapper sliderMapper;

	private final MessageSource messageSource;

	private final ObjectMapper objectMapper;

	public ShopScreenSliderSaveService(
			SliderMapper sliderMapper, MessageSource messageSource, ObjectMapper objectMapper) {
		this.sliderMapper = sliderMapper;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> saveSlider(long companyId, Map<String, Object> merged) {
		Locale locale = LocaleContextHolder.getLocale();
		validateRequiredFieldsInOrder(merged, locale);

		long distributorId = parseLongDefaultAllowMissing(merged.get("distributor_id"), 0L);

		Slider existing =
				sliderMapper.selectOne(
						new LambdaQueryWrapper<Slider>()
								.eq(Slider::getCompanyId, companyId)
								.eq(Slider::getDistributorId, distributorId));

		if (existing == null) {
			return insertNew(companyId, distributorId, merged, locale);
		}
		return updateExisting(companyId, distributorId, merged, locale);
	}

	private void validateRequiredFieldsInOrder(Map<String, Object> merged, Locale locale) {
		if (requiredStringInvalid(merged.get("title"))) {
			throw new BadRequestException(
					messageSource.getMessage("distribution.shop_screen.validation.title_required", null, locale));
		}
		if (requiredStringInvalid(merged.get("sub_title"))) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.shop_screen.validation.sub_title_required", null, locale));
		}
		if (mixedJsonFieldRequiredInvalid(merged.get("style_params"))) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.shop_screen.validation.style_params_required", null, locale));
		}
		if (mixedJsonFieldRequiredInvalid(merged.get("image_list"))) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.shop_screen.validation.image_list_required", null, locale));
		}
	}

	private boolean mixedJsonFieldRequiredInvalid(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			try {
				JsonNode node = objectMapper.readTree(t);
				if (node.isNull()) {
					return true;
				}
				if (node.isObject() && node.size() == 0) {
					return true;
				}
				if (node.isArray() && node.size() == 0) {
					return true;
				}
			} catch (JsonProcessingException ex) {
				return true;
			}
			return false;
		}
		if (raw instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof JsonNode j) {
			if (j.isNull()) {
				return true;
			}
			if (j.isObject() && j.size() == 0) {
				return true;
			}
			return j.isArray() && j.size() == 0;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		return false;
	}

	private Map<String, Object> insertNew(
			long companyId, long distributorId, Map<String, Object> merged, Locale locale) {
		Slider row = new Slider();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setTitle(String.valueOf(merged.get("title")).trim());
		row.setSubTitle(String.valueOf(merged.get("sub_title")).trim());
		row.setStyleParams(toJsonColumn(merged.get("style_params"), locale));
		row.setImageList(toJsonColumn(merged.get("image_list"), locale));
		if (merged.containsKey("desc_status")) {
			row.setDescStatus(!descStatusInputFalsy(merged.get("desc_status")));
		} else {
			row.setDescStatus(Boolean.FALSE);
		}
		sliderMapper.insert(row);
		return SliderColumnNamesDataMapper.toColumnNamesData(
				row,
				objectMapper,
				merged.get("style_params") instanceof String,
				merged.get("image_list") instanceof String);
	}

	private Map<String, Object> updateExisting(
			long companyId, long distributorId, Map<String, Object> merged, Locale locale) {
		Slider entity =
				sliderMapper.selectOne(
						new LambdaQueryWrapper<Slider>()
								.eq(Slider::getCompanyId, companyId)
								.eq(Slider::getDistributorId, distributorId));
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("distribution.repositories.no_update_data_found", null, locale));
		}
		if (merged.containsKey("title")) {
			entity.setTitle(String.valueOf(merged.get("title")).trim());
		}
		if (merged.containsKey("sub_title")) {
			entity.setSubTitle(String.valueOf(merged.get("sub_title")).trim());
		}
		if (merged.containsKey("style_params")) {
			entity.setStyleParams(toJsonColumn(merged.get("style_params"), locale));
		}
		if (merged.containsKey("image_list")) {
			entity.setImageList(toJsonColumn(merged.get("image_list"), locale));
		}
		if (merged.containsKey("desc_status")) {
			entity.setDescStatus(!descStatusInputFalsy(merged.get("desc_status")));
		}
		if (merged.containsKey("company_id")) {
			entity.setCompanyId(parseLongStrictPositive(merged.get("company_id"), "company_id"));
		}
		if (merged.containsKey("distributor_id")) {
			entity.setDistributorId(parseLongDefaultAllowMissing(merged.get("distributor_id"), 0L));
		}
		if (merged.containsKey("slide_id") && merged.get("slide_id") != null) {
			entity.setSlideId(parseLongStrict(merged.get("slide_id"), "slide_id"));
		}
		sliderMapper.updateById(entity);
		boolean styleParamsEchoString =
				merged.containsKey("style_params") && merged.get("style_params") instanceof String;
		boolean imageListEchoString =
				merged.containsKey("image_list") && merged.get("image_list") instanceof String;
		return SliderColumnNamesDataMapper.toColumnNamesData(
				entity, objectMapper, styleParamsEchoString, imageListEchoString);
	}

	private String toJsonColumn(Object raw, Locale locale) {
		if (raw instanceof String s) {
			String t = s.trim();
			try {
				JsonNode node = objectMapper.readTree(t);
				return objectMapper.writeValueAsString(node);
			} catch (JsonProcessingException ex) {
				throw new BadRequestException(
						messageSource.getMessage(
								"distribution.shop_screen.validation.slider_column_json_invalid", null, locale));
			}
		}
		if (raw instanceof Map<?, ?> || raw instanceof Collection<?> || raw instanceof JsonNode) {
			try {
				return objectMapper.writeValueAsString(raw);
			} catch (JsonProcessingException ex) {
				throw new BadRequestException(
						messageSource.getMessage(
								"distribution.shop_screen.validation.slider_column_json_invalid", null, locale));
			}
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException ex) {
			throw new BadRequestException(
					messageSource.getMessage(
							"distribution.shop_screen.validation.slider_column_json_invalid", null, locale));
		}
	}

	private static boolean requiredStringInvalid(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		String s = String.valueOf(raw).trim();
		return !StringUtils.hasText(s) || "0".equals(s);
	}

	private static boolean descStatusInputFalsy(Object rs) {
		if (rs == null) {
			return true;
		}
		if (rs instanceof Boolean b) {
			return !b.booleanValue();
		}
		if (rs instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (rs instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			if ("0".equals(t)) {
				return true;
			}
			return "false".equalsIgnoreCase(t);
		}
		return false;
	}

	private static long parseLongDefaultAllowMissing(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("distributor_id 格式错误");
		}
	}

	private long parseLongStrict(Object o, String fieldKey) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(fieldKey + " 格式错误");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException ex) {
			throw new BadRequestException(fieldKey + " 格式错误");
		}
	}

	private long parseLongStrictPositive(Object o, String fieldKey) {
		long v = parseLongStrict(o, fieldKey);
		if (v <= 0L) {
			throw new BadRequestException(fieldKey + " 无效");
		}
		return v;
	}
}
