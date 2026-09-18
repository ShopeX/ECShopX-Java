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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 门店 id 在 Controller 于本校验通过后再检查，避免与其它参数错误混在同一批提示中。 */
@Service
public class ReservationListParamValidator {

	private static final String MSG_PAGE = "分页页码为整数|最小为1";
	private static final String MSG_PAGE_SIZE = "分页每页数据为整数|最小为1|最大100";
	private static final String MSG_DATE_DAY = "日期必填";

	public ReservationListValidatedParams validate(Map<String, String> queryLike) {
		List<String> errs = new ArrayList<>();
		int page = 1;
		int pageSize = 50;

		if (queryLike != null && queryLike.containsKey("page")) {
			String raw = queryLike.get("page");
			if (!StringUtils.hasText(raw == null ? "" : raw.trim())) {
				errs.add(MSG_PAGE);
			} else {
				try {
					int p = Integer.parseInt(raw.trim());
					if (p < 1) {
						errs.add(MSG_PAGE);
					} else {
						page = p;
					}
				} catch (NumberFormatException e) {
					errs.add(MSG_PAGE);
				}
			}
		}

		if (queryLike != null && queryLike.containsKey("pageSize")) {
			String raw = queryLike.get("pageSize");
			if (!StringUtils.hasText(raw == null ? "" : raw.trim())) {
				errs.add(MSG_PAGE_SIZE);
			} else {
				try {
					int ps = Integer.parseInt(raw.trim());
					if (ps < 1 || ps > 100) {
						errs.add(MSG_PAGE_SIZE);
					} else {
						pageSize = ps;
					}
				} catch (NumberFormatException e) {
					errs.add(MSG_PAGE_SIZE);
				}
			}
		}

		String dateDayRaw = queryLike == null ? null : queryLike.get("dateDay");
		if (!StringUtils.hasText(dateDayRaw == null ? "" : dateDayRaw.trim())) {
			errs.add(MSG_DATE_DAY);
		}

		if (!errs.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String e : errs) {
				sb.append(e).append("，");
			}
			throw new ResourceException(sb.toString());
		}

		return new ReservationListValidatedParams(page, pageSize, dateDayRaw.trim());
	}
}
