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

public interface OpenapiEnabledLogisticsListPort {

	/**
	 * 返回已启用物流公司列表（OpenAPI data 数组）。
	 * 首项恒为 OTHER/其他；后续项仅含 corp_code、corp_name。
	 */
	List<Map<String, Object>> listEnabled(long companyId, int distributorId, int supplierId);
}
