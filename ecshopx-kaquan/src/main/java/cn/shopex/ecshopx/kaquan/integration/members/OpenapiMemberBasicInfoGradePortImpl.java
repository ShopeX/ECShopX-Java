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

package cn.shopex.ecshopx.kaquan.integration.members;

import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeSingleRowQueryService;
import cn.shopex.ecshopx.members.integration.kaquan.OpenapiMemberBasicInfoGradePort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberBasicInfoGradePortImpl implements OpenapiMemberBasicInfoGradePort {

	private final MemberCardGradeSingleRowQueryService memberCardGradeSingleRowQueryService;

	public OpenapiMemberBasicInfoGradePortImpl(
			MemberCardGradeSingleRowQueryService memberCardGradeSingleRowQueryService) {
		this.memberCardGradeSingleRowQueryService = memberCardGradeSingleRowQueryService;
	}

	@Override
	public Map<String, Object> getGradeByGradeIdForOpenapi(long companyId, long gradeId) {
		Map<String, Object> loaded =
				memberCardGradeSingleRowQueryService.loadForAdmin(companyId, gradeId, "zh-CN");
		if (loaded == null || loaded.isEmpty()) {
			return null;
		}
		return toOpenapiGradeRow(loaded);
	}

	private static LinkedHashMap<String, Object> toOpenapiGradeRow(Map<String, Object> loaded) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("grade_id", loaded.get("grade_id"));
		row.put("company_id", loaded.get("company_id"));
		row.put("grade_name", loaded.get("grade_name"));
		row.put("default_grade", loaded.get("default_grade"));
		row.put("background_pic_url", loaded.get("background_pic_url"));
		row.put("promotion_condition", loaded.get("promotion_condition"));
		row.put("privileges", loaded.get("privileges"));
		row.put("description", loaded.get("description"));
		row.put("grade_background", loaded.get("grade_background"));
		return row;
	}
}
