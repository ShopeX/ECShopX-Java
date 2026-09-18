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

package cn.shopex.ecshopx.kaquan.integration.openapi;

import cn.shopex.ecshopx.common.kaquan.port.OpenapiMemberCardGradeByExternalIdPort;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiMemberCardGradeByExternalIdPortImpl implements OpenapiMemberCardGradeByExternalIdPort {

	private static final ObjectMapper GRADE_JSON = new ObjectMapper();

	private final MemberCardGradeMapper memberCardGradeMapper;

	public OpenapiMemberCardGradeByExternalIdPortImpl(MemberCardGradeMapper memberCardGradeMapper) {
		this.memberCardGradeMapper = memberCardGradeMapper;
	}

	@Override
	public Map<String, Object> findByExternalId(long companyId, String externalId) {
		if (!StringUtils.hasText(externalId) || !StringUtils.hasText(externalId.trim())) {
			return null;
		}
		MemberCardGrade entity =
				memberCardGradeMapper.selectOne(
						new LambdaQueryWrapper<MemberCardGrade>()
								.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId))
								.eq(MemberCardGrade::getExternalId, externalId.trim())
								.last("LIMIT 1"));
		if (entity == null) {
			return null;
		}
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("grade_id", entity.getGradeId());
		row.put("grade_name", entity.getGradeName());
		row.put("promotion_condition", decodePromotionConditionValue(entity.getPromotionCondition()));
		return row;
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
