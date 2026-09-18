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

public final class GoodsDataExportRequestParams {

	private GoodsDataExportRequestParams() {}

	/** Export flag semantics aligned with the admin goods data endpoint. */
	public static boolean isExportTrue(HttpServletRequest request) {
		String raw = request.getParameter("export");
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		try {
			return Integer.parseInt(t) != 0;
		} catch (NumberFormatException e) {
			return true;
		}
	}
}
