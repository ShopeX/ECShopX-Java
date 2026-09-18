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

public interface OpenapiDistributorDetailPort {

	/**
	 * 按 shop_code 加载店铺详情并完成 OpenAPI 字段映射。
	 *
	 * @param shopCodeRaw originalString 合并结果（已通过 Handler 非 null/非空串校验；可为含空白字符串）
	 */
	Map<String, Object> getDistributorDetail(long companyId, String shopCodeRaw);
}
