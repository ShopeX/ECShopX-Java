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

import java.util.List;
import java.util.Map;

public interface OpenapiMemberBatchCreatePort {

	/**
	 * V2 ecx.member.batch_create。
	 *
	 * @param companyId 鉴权企业 ID
	 * @param hasDataKey 对齐 PHP $request->has("data")；false 时直接返回 status=0（items 忽略）
	 * @param items 已由 Controller 解析的 data JSON 数组元素；hasDataKey=true 时传入，解码失败/空串时为 List.of()
	 */
	Map<String, Object> batchCreate(long companyId, boolean hasDataKey, List<Map<String, Object>> items);
}
