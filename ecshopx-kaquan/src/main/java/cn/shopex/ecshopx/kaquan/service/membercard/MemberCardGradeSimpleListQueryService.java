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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberCardGradeSimpleListQueryService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;

	public MemberCardGradeSimpleListQueryService(
			MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeMultiLangReadService = memberCardGradeMultiLangReadService;
	}

	public List<Map<String, Object>> getCompanyGradeSimpleList(long companyId, String lang) {
		String companyIdStr = String.valueOf(companyId);
		List<MemberCardGrade> entities = memberCardGradeMapper.selectList(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr)
						.select(MemberCardGrade::getGradeId, MemberCardGrade::getGradeName));
		if (entities == null || entities.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		for (MemberCardGrade entity : entities) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("grade_id", entity.getGradeId());
			row.put("grade_name", entity.getGradeName());
			rows.add(row);
		}
		memberCardGradeMultiLangReadService.applyOverlays(companyId, rows, lang);
		for (Map<String, Object> row : rows) {
			Object gradeId = row.get("grade_id");
			if (gradeId != null) {
				row.put("grade_id", String.valueOf(gradeId));
			}
		}
		return rows;
	}
}
