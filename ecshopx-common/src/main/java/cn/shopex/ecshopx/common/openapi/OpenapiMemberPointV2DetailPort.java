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

public interface OpenapiMemberPointV2DetailPort {

	/**
	 * V2 ecx.member_point.detail：按 mobile 查询会员可用积分。
	 *
	 * @param companyId 鉴权后的企业 ID
	 * @param mobileRaw Query+Body 合并后的 mobile 原始值（Service 内校验）
	 * @return {@code Map.of("point", intValue)}；无会员或无 point_member 行时 point=0
	 */
	Map<String, Object> getMemberPointDetail(long companyId, String mobileRaw);
}
