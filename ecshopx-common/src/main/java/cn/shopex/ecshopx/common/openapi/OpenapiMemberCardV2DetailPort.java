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

public interface OpenapiMemberCardV2DetailPort {

	/**
	 * V2 ecx.member_card.detail：按 company_id 查询会员卡基础设置（OpenAPI 格式化后）。
	 *
	 * @param companyId 鉴权后的企业 ID
	 * @return 固定 7 键 Map；无 membercard 行时各值为 ""
	 */
	Map<String, Object> getMemberCardDetail(long companyId);
}
