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

public interface OpenapiMemberV2DetailPort {

	/**
	 * V2 ecx.member.detail：按 mobile 查单条会员详情（未经 handleDataToList 格式化）。
	 *
	 * @param companyId   鉴权后的企业 ID
	 * @param mobilePlain 已合并 Query/Body 的 mobile 原始字符串（校验在实现内完成）
	 * @return 单条会员 Map；企业内无匹配时返回 {@code null}（信封 data:null）
	 */
	Map<String, Object> getMemberDetail(long companyId, String mobilePlain);
}
