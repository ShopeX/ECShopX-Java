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

package cn.shopex.ecshopx.orders.service.admin.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class TradeListTimeStartFromRequest {

	private TradeListTimeStartFromRequest() {}

	public static TradeListTimeStartParams resolve(HttpServletRequest request) {
		String timeStartBegin = null;
		String timeStartEnd = null;

		String dateBegin = request.getParameter("date_begin");
		if (StringUtils.hasText(dateBegin)) {
			timeStartBegin = dateBegin.trim();
			String dateEnd = request.getParameter("date_end");
			if (StringUtils.hasText(dateEnd)) {
				timeStartEnd = dateEnd.trim();
			} else {
				timeStartEnd = null;
			}
		}

		String tsb = request.getParameter("time_start_begin");
		if (StringUtils.hasText(tsb)) {
			timeStartBegin = tsb.trim();
			String tse = request.getParameter("time_start_end");
			if (StringUtils.hasText(tse)) {
				timeStartEnd = tse.trim();
			} else {
				timeStartEnd = null;
			}
		}

		return new TradeListTimeStartParams(timeStartBegin, timeStartEnd);
	}
}
