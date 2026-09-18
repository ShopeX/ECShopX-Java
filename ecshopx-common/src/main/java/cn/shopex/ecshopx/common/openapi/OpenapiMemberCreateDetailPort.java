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

public interface OpenapiMemberCreateDetailPort {

	/**
	 * V2 ecx.member.create 完整创建。
	 *
	 * @param companyId 鉴权后的企业 ID
	 * @param rawParams Handler 合并后的业务参数字符串 Map（键为 OpenAPI 对外名，值多为 String；habbit 可为 List/Map）
	 */
	Map<String, Object> createDetail(long companyId, Map<String, Object> rawParams);
}
