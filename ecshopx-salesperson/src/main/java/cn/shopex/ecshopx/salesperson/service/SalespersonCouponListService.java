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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.dto.SalespersonCouponListRowDto;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonCouponListMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonCouponListService {

	private final SalespersonCouponListMapper salespersonCouponListMapper;
	private final SalespersonCouponGrantSettingRedisService grantSettingRedisService;

	public SalespersonCouponListService(
			SalespersonCouponListMapper salespersonCouponListMapper,
			SalespersonCouponGrantSettingRedisService grantSettingRedisService) {
		this.salespersonCouponListMapper = salespersonCouponListMapper;
		this.grantSettingRedisService = grantSettingRedisService;
	}

	public Map<String, Object> lists(long companyId, String page, String pageSize) {
		Integer pageInt = parseOptionalInt(trimToNull(page));
		Integer pageSizeInt = parseOptionalInt(trimToNull(pageSize));

		boolean applyPage = pageInt != null && pageInt > 0;
		long offset = 0L;
		Long limit = null;
		if (applyPage) {
			if (pageSizeInt != null) {
				offset = (long) (pageInt - 1) * pageSizeInt;
			}
			if (pageSizeInt != null && pageSizeInt > 0) {
				limit = pageSizeInt.longValue();
			}
		}

		long totalCount = salespersonCouponListMapper.countJoinedByCompanyId(companyId);

		List<SalespersonCouponListRowDto> list;
		if (totalCount == 0L) {
			list = Collections.emptyList();
		} else {
			List<Map<String, Object>> rawRows;
			if (limit != null) {
				rawRows = salespersonCouponListMapper.selectJoinedRows(companyId, offset, limit);
			} else {
				rawRows = salespersonCouponListMapper.selectJoinedRows(companyId, null, null);
			}
			list = rawRows.stream().map(SalespersonCouponListRowDto::fromRow).toList();
		}

		Map<String, String> grantMap = grantSettingRedisService.readGrantSettingHash(companyId);
		Object couponSetting = grantMap.isEmpty() ? Collections.emptyList() : grantMap;

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", totalCount);
		body.put("list", list);
		body.put("coupon_setting", couponSetting);
		return body;
	}

	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	/**
	 * 非空字符串且为十进制整数时返回其值，否则 null。
	 */
	private static Integer parseOptionalInt(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
