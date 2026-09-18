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

public interface OpenapiMemberV2ListPort {

	/**
	 * V2 ecx.member.list 多条件分页列表。
	 *
	 * @param companyId 鉴权后的企业 ID
	 * @param page      有效页码（≥1）
	 * @param pageSize  有效页大小（≥20，≤499）
	 * @param rawParams Handler 合并后的业务参数（仅含「请求存在」的白名单键；值为 String/Object）
	 */
	Map<String, Object> listMembers(long companyId, int page, int pageSize, Map<String, Object> rawParams);
}
