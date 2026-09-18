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

package cn.shopex.ecshopx.merchant.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantSettlementApplyListRequestParamsResolver {

	public Map<String, Object> resolve(HttpServletRequest request) {
		Map<String, Object> out = new LinkedHashMap<>();
		putSingleOrList(out, "page", request.getParameterValues("page"));
		putSingleOrList(out, "page_size", request.getParameterValues("page_size"));
		putSingleOrList(out, "audit_status", request.getParameterValues("audit_status"));
		putSingleOrList(out, "merchant_name", request.getParameterValues("merchant_name"));
		putSingleOrList(out, "province", request.getParameterValues("province"));
		putSingleOrList(out, "city", request.getParameterValues("city"));
		putSingleOrList(out, "area", request.getParameterValues("area"));
		putSingleOrList(out, "settled_type", request.getParameterValues("settled_type"));
		String[] ts = request.getParameterValues("time_start");
		if (ts == null || ts.length == 0) {
			ts = request.getParameterValues("time_start[]");
		}
		if (ts != null && ts.length > 0) {
			out.put("time_start", new ArrayList<>(Arrays.asList(ts)));
		}
		return out;
	}

	private static void putSingleOrList(Map<String, Object> out, String key, String[] vals) {
		if (vals == null || vals.length == 0) {
			return;
		}
		if (vals.length == 1) {
			out.put(key, vals[0]);
		} else {
			out.put(key, new ArrayList<>(Arrays.asList(vals)));
		}
	}
}
