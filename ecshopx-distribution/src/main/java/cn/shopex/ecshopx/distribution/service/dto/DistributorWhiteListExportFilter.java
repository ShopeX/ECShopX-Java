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
 * Normalized filter for whitelist export queries (grouped by mobile).
 *
 * @param companyId company scope
 * @param distributorIds when non-null and non-empty, restricts rows to these distributor ids
 * @param mobileFilterActive when true, applies {@code mobile} equality (including empty string)
 * @param mobile mobile value used only if {@code mobileFilterActive}
 * @param usernamePrefix when non-null and non-blank after trim, applies {@code username LIKE prefix%}
 * @param shopNotFound when true, queries must yield zero rows
 */
public record DistributorWhiteListExportFilter(
		long companyId,
		List<Long> distributorIds,
		boolean mobileFilterActive,
		String mobile,
		String usernamePrefix,
		boolean shopNotFound) {}
