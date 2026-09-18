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

package cn.shopex.ecshopx.common.openapi;

import java.util.Map;

public interface OpenapiMemberV2ListByGradeOrVipGradePort {

	/**
	 * V2 ecx.member_card_grade_rel.list：按 grade_id 或 vip_grade_id（二选一）分页查关联会员。
	 *
	 * @param companyId          鉴权后的企业 ID
	 * @param gradeIdPresent     {@code isParamPresent(gradeIdParam, body, "grade_id")}
	 * @param gradeIdRaw         {@code originalString(...)}；仅 gradeIdPresent 时参与校验
	 * @param vipGradeIdPresent  {@code isParamPresent(vipGradeIdParam, body, "vip_grade_id")}
	 * @param vipGradeIdRaw      {@code originalString(...)}；仅 vipGradeIdPresent 时参与校验
	 * @param page               有效页码（≥1）
	 * @param pageSize           有效页大小（≥20，≤499）
	 */
	Map<String, Object> listByGradeOrVipGrade(
			long companyId,
			boolean gradeIdPresent,
			String gradeIdRaw,
			boolean vipGradeIdPresent,
			String vipGradeIdRaw,
			int page,
			int pageSize);
}
