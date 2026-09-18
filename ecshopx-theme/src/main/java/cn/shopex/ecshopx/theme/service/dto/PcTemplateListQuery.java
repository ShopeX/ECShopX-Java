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

package cn.shopex.ecshopx.theme.service.dto;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public record PcTemplateListQuery(
		long companyId,
		int distributorId,
		String pageTypeFilterOrNull,
		String statusFilterRawOrNull,
		int pageNo,
		int pageSize) {

	public static PcTemplateListQuery fromHttpServletRequest(
			HttpServletRequest request, long companyId, int pageNo, int pageSize) {
		String pageTypeFilterOrNull = null;
		String pageTypeRaw = request.getParameter("page_type");
		if (pageTypeRaw != null) {
			String t = pageTypeRaw.trim();
			if (StringUtils.hasText(t)) {
				pageTypeFilterOrNull = t;
			}
		}

		String statusFilterRawOrNull = null;
		String statusRaw = request.getParameter("status");
		if (statusRaw != null) {
			String st = statusRaw.trim();
			if (StringUtils.hasText(st) && !"0".equals(st)) {
				statusFilterRawOrNull = st;
			}
		}

		int distributorId = 0;
		String distributorRaw = request.getParameter("distributor_id");
		if (distributorRaw != null && StringUtils.hasText(distributorRaw.trim())) {
			try {
				distributorId = Math.max(0, Integer.parseInt(distributorRaw.trim()));
			} catch (NumberFormatException ignored) {
				distributorId = 0;
			}
		}

		return new PcTemplateListQuery(
				companyId, distributorId, pageTypeFilterOrNull, statusFilterRawOrNull, pageNo, pageSize);
	}
}
