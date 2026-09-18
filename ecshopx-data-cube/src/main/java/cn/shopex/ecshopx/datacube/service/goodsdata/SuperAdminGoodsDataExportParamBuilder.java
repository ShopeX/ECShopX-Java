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

package cn.shopex.ecshopx.datacube.service.goodsdata;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminGoodsDataExportParamBuilder {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	public AdminGoodsDataFilter buildForItPlatformExport(HttpServletRequest request, long companyId) {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		LocalDate start = AdminGoodsDataParamBuilder.resolveDate(request.getParameter("start"), yesterday);
		LocalDate end = AdminGoodsDataParamBuilder.resolveDate(request.getParameter("end"), yesterday);
		AdminGoodsDataParamBuilder.validateDateRange(start, end);

		return new AdminGoodsDataFilter(
				companyId,
				start.format(ISO_DATE),
				end.format(ISO_DATE),
				false,
				"",
				List.of(),
				null,
				0L);
	}
}
