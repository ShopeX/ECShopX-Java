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

package cn.shopex.ecshopx.distribution.service.dto;

import java.util.List;

/**
 * Parsed input for creating a logistics-type distributor after-sales address (admin POST path).
 */
public record DistributorAftersalesAddressSetLogisticsInput(
		long companyId,
		List<Long> distributorIds,
		String distributorIdRaw,
		String province,
		String city,
		String area,
		String regionsId,
		String regions,
		String address,
		String mobile,
		String contact,
		long merchantId,
		int supplierId) {
}
