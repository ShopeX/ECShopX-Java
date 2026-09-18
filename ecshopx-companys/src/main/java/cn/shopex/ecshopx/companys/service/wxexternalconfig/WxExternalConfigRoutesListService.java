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

package cn.shopex.ecshopx.companys.service.wxexternalconfig;

import cn.shopex.ecshopx.companys.mapper.WxExternalRoutesMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxExternalConfigRoutesListService {

	private static final String[] ROUTE_ROW_KEYS = {
		"wx_external_config_id",
		"app_id",
		"app_name",
		"app_desc",
		"wx_external_routes_id",
		"route_name",
		"route_info",
		"route_desc"
	};

	private final WxExternalRoutesMapper wxExternalRoutesMapper;

	public WxExternalConfigRoutesListService(WxExternalRoutesMapper wxExternalRoutesMapper) {
		this.wxExternalRoutesMapper = wxExternalRoutesMapper;
	}

	public Map<String, Object> getConfigRoutesList(
			long companyId, String appId, String routeInfo, int page, int pageSize) {
		int p = page < 1 ? 1 : page;
		int ps = pageSize;

		String routeInfoNeedleEscaped = routeInfo == null ? null : escapeLike(routeInfo);
		Long total = wxExternalRoutesMapper.countConfigRoutesList(companyId, appId, routeInfoNeedleEscaped);
		long totalCount = total == null ? 0L : total;

		if (totalCount == 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		List<LinkedHashMap<String, Object>> rawRows;
		if (ps > 0) {
			int offset = (p - 1) * ps;
			rawRows = wxExternalRoutesMapper.selectConfigRoutesList(
					companyId, appId, routeInfoNeedleEscaped, ps, offset, true);
		} else {
			rawRows = wxExternalRoutesMapper.selectConfigRoutesList(
					companyId, appId, routeInfoNeedleEscaped, null, null, false);
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (LinkedHashMap<String, Object> row : rawRows) {
			listMaps.add(mapRouteRow(row));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", listMaps);
		return data;
	}

	private static LinkedHashMap<String, Object> mapRouteRow(LinkedHashMap<String, Object> row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String key : ROUTE_ROW_KEYS) {
			Object v = row.get(key);
			if (v == null) {
				out.put(key, null);
			} else if (v instanceof String) {
				out.put(key, (String) v);
			} else {
				out.put(key, String.valueOf(v));
			}
		}
		return out;
	}

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
