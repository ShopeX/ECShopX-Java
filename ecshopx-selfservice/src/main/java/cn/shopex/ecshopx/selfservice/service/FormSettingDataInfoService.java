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

import cn.shopex.ecshopx.selfservice.domain.FormSetting;
import cn.shopex.ecshopx.selfservice.mapper.FormSettingMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormSettingOutsideMultiLangReadService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FormSettingDataInfoService {

	private final FormSettingMapper formSettingMapper;
	private final FormSettingOutsideMultiLangReadService multiLangReadService;
	private final FormSettingApiRowAssembler rowAssembler;

	public FormSettingDataInfoService(
			FormSettingMapper formSettingMapper,
			FormSettingOutsideMultiLangReadService multiLangReadService,
			FormSettingApiRowAssembler rowAssembler) {
		this.formSettingMapper = formSettingMapper;
		this.multiLangReadService = multiLangReadService;
		this.rowAssembler = rowAssembler;
	}

	public Object getDataInfo(String idRaw, String countryCode) {
		boolean falsy =
				(idRaw == null)
						|| (!StringUtils.hasText(idRaw.trim()))
						|| ("0".equals(idRaw.trim()));
		if (falsy) {
			return Collections.emptyList();
		}

		String t = idRaw.trim();
		Long id;
		try {
			id = Long.parseLong(t);
		} catch (NumberFormatException ex) {
			return Collections.emptyList();
		}

		FormSetting entity = formSettingMapper.selectById(id);
		if (entity == null) {
			return Collections.emptyList();
		}

		Map<String, Object> row = rowAssembler.toDataInfoRow(entity);

		String langTag = resolveLangTag(countryCode);
		List<Map<String, Object>> one = List.of(row);
		multiLangReadService.applyFieldTitleOverrides(entity.getCompanyId(), one, langTag);

		return row;
	}

	private static String resolveLangTag(String countryCode) {
		if (countryCode == null || !StringUtils.hasText(countryCode.trim())) {
			return "zh-CN";
		}
		String trimmed = countryCode.trim();
		return StringUtils.hasText(trimmed) ? trimmed : "zh-CN";
	}
}
