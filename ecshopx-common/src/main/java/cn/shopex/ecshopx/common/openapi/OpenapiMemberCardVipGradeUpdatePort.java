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
import java.util.Optional;

public interface OpenapiMemberCardVipGradeUpdatePort {

	/**
	 * V2 ecx.member_card_vip_grade.update：部分更新付费会员卡等级。
	 *
	 * @return 15 字段 Map；恒返回对象（含仅 vip_grade_id 场景；禁止 null）
	 */
	Map<String, Object> update(
			long companyId,
			String vipGradeIdRaw,
			Optional<String> gradeNamePresent,
			Optional<String> monthlyFeePresent,
			Optional<String> quarterFeePresent,
			Optional<String> yearFeePresent,
			Optional<String> discountPresent,
			Optional<String> guideTitlePresent,
			Optional<String> descriptionPresent,
			Optional<String> isDefaultPresent,
			Optional<String> isDisabledPresent,
			Optional<String> externalIdPresent);
}
