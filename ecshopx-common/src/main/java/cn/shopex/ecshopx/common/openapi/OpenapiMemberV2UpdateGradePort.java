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

public interface OpenapiMemberV2UpdateGradePort {

	/**
	 * V2 ecx.member.card_grade.update：按 plat_account(user_id) 定位会员，
	 * 请求 grade_id 作 external_id 解析内部等级并更新；条件触发 member_upgrade 营销。
	 *
	 * @param companyId 鉴权后的企业 ID
	 * @param mergedRaw mergeAll 结果（含 plat_account、grade_id、grade_level）
	 */
	void updateGrade(long companyId, Map<String, Object> mergedRaw);
}
