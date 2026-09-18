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

package cn.shopex.ecshopx.kaquan.service.membercard;

import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberCardGradeSingleRowQueryService {

	private static final ObjectMapper GRADE_JSON = new ObjectMapper();

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;

	public MemberCardGradeSingleRowQueryService(
			MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeMultiLangReadService = memberCardGradeMultiLangReadService;
	}

	public Map<String, Object> loadForAdmin(long companyId, long gradeId, String lang) {
		String companyIdStr = String.valueOf(companyId);
		MemberCardGrade entity = memberCardGradeMapper.selectOne(new LambdaQueryWrapper<MemberCardGrade>()
				.eq(MemberCardGrade::getCompanyId, companyIdStr)
				.eq(MemberCardGrade::getGradeId, gradeId)
				.last("LIMIT 1"));
		if (entity == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> row = buildBaseRow(entity);
		List<Map<String, Object>> list = List.of(row);
		memberCardGradeMultiLangReadService.applyOverlays(companyId, list, lang);
		return list.get(0);
	}

	private static Map<String, Object> buildBaseRow(MemberCardGrade entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", entity.getCompanyId());
		row.put("grade_id", entity.getGradeId());
		row.put("grade_name", entity.getGradeName());
		row.put("default_grade", Boolean.TRUE.equals(entity.getDefaultGrade()));
		row.put("background_pic_url", entity.getBackgroundPicUrl());
		row.put("grade_background", entity.getGradeBackground());
		row.put("promotion_condition", decodePromotionConditionValue(entity.getPromotionCondition()));
		row.put("privileges", decodePrivilegesValue(entity.getPrivileges()));
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("third_data", entity.getThirdData());
		row.put("external_id", entity.getExternalId());
		row.put("description", entity.getDescription());
		row.put("dm_grade_code", entity.getDmGradeCode());
		return row;
	}

	private static Object decodePrivilegesValue(String raw) {
		if (raw == null || raw.isBlank()) {
			return new LinkedHashMap<String, Object>();
		}
		try {
			Object v = GRADE_JSON.readValue(raw.trim(), new TypeReference<Object>() {});
			if (v instanceof Map<?, ?> || v instanceof List<?>) {
				return v;
			}
			return new LinkedHashMap<String, Object>();
		} catch (Exception e) {
			return new LinkedHashMap<String, Object>();
		}
	}

	private static Object decodePromotionConditionValue(String raw) {
		if (raw == null || raw.isBlank()) {
			return new ArrayList<Object>();
		}
		try {
			Object v = GRADE_JSON.readValue(raw.trim(), new TypeReference<Object>() {});
			if (v == null) {
				return new ArrayList<Object>();
			}
			if (v instanceof List<?> || v instanceof Map<?, ?>) {
				return v;
			}
			return new ArrayList<Object>();
		} catch (Exception e) {
			return new ArrayList<Object>();
		}
	}
}
