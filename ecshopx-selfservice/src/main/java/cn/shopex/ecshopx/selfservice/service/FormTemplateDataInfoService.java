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

import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangReadService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FormTemplateDataInfoService {

	private final FormTemplateMapper formTemplateMapper;
	private final FormTemplateApiRowAssembler formTemplateApiRowAssembler;
	private final FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService;

	public FormTemplateDataInfoService(
			FormTemplateMapper formTemplateMapper,
			FormTemplateApiRowAssembler formTemplateApiRowAssembler,
			FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService) {
		this.formTemplateMapper = formTemplateMapper;
		this.formTemplateApiRowAssembler = formTemplateApiRowAssembler;
		this.formTemplateOutsideMultiLangReadService = formTemplateOutsideMultiLangReadService;
	}

	public Object getDataInfo(String idRaw, String countryCode) {
		String t = idRaw == null ? "" : idRaw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return Collections.emptyList();
		}
		long id;
		try {
			id = Long.parseLong(t);
		} catch (NumberFormatException ex) {
			return Collections.emptyList();
		}
		if (id <= 0L) {
			return Collections.emptyList();
		}

		FormTemplate entity = formTemplateMapper.selectById(id);
		if (entity == null) {
			return Collections.emptyList();
		}

		Map<String, Object> row = formTemplateApiRowAssembler.toRow(entity);

		String requestLangTag = resolveLangTag(countryCode);
		formTemplateOutsideMultiLangReadService.applyListLangOverrides(
				entity.getCompanyId(), List.of(row), requestLangTag);

		// Multilang may replace decoded content with a JSON string; decode again so the detail response stays structured.
		Object content = row.get("content");
		if (content instanceof String s) {
			formTemplateApiRowAssembler
					.tryDecodeMultilangJsonField(s)
					.ifPresent(decoded -> row.put("content", decoded));
		}

		return row;
	}

	private static String resolveLangTag(String countryCode) {
		if (countryCode == null || !StringUtils.hasText(countryCode.trim())) {
			return "zh-CN";
		}
		String t = countryCode.trim();
		return StringUtils.hasText(t) ? t : "zh-CN";
	}
}
