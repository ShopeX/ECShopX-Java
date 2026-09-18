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

package cn.shopex.ecshopx.common.kaquan.port;

import java.util.Map;

public interface OpenapiMemberCardGradeByExternalIdPort {

	/**
	 * 按 company_id + external_id 查 membercard_grade 单行。
	 * 无匹配返回 null。返回 Map 至少含 grade_id、grade_name、promotion_condition（已解码）。
	 */
	Map<String, Object> findByExternalId(long companyId, String externalId);
}
