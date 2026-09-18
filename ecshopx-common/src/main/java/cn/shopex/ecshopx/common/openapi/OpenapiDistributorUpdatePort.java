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

public interface OpenapiDistributorUpdatePort {

	/**
	 * V2 ecx.distributor.update：部分更新店铺，返回 22 字段 OpenAPI 单对象。
	 * Action 入口成功路径恒为单对象（无 data=null）。
	 */
	Map<String, Object> update(
			long companyId,
			String shopCodeRaw,
			Optional<String> distributorNamePresent,
			Optional<String> contactUsernamePresent,
			Optional<String> contactMobilePresent,
			Optional<String> hourPresent,
			Optional<String> isZitiPresent,
			Optional<String> isDeliveryPresent,
			Optional<String> isAutoSyncGoodsPresent,
			Optional<String> isDadaPresent,
			Optional<String> isDefaultPresent,
			Optional<String> logoPresent,
			Optional<String> statusPresent,
			Optional<String> provincePresent,
			Optional<String> cityPresent,
			Optional<String> areaPresent,
			Optional<String> addressPresent,
			Optional<String> lngPresent,
			Optional<String> latPresent);
}
