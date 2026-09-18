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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Monitors;
import cn.shopex.ecshopx.datacube.mapper.MonitorsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MonitorsListService {

	private static final Pattern INTEGER_STRING = Pattern.compile("^[+\\-]?\\d+$");

	private static final String MSG = "获取监控列表出错.";

	private final MonitorsMapper monitorsMapper;

	private final RegionauthMapper regionauthMapper;

	public MonitorsListService(MonitorsMapper monitorsMapper, RegionauthMapper regionauthMapper) {
		this.monitorsMapper = monitorsMapper;
		this.regionauthMapper = regionauthMapper;
	}

	public Map<String, Object> getMonitorsList(Map<String, Object> filter, String pageRaw, String pageSizeRaw) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		validatePage(pageRaw, fieldErrors);
		validatePageSize(pageSizeRaw, fieldErrors);
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException(MSG);
		}

		int page = parseValidatedPositiveInt(pageRaw.trim());
		int pageSize = parseValidatedPageSizeInt(pageSizeRaw.trim());

		page = page < 1 ? 1 : page;
		pageSize = pageSize > 100 ? 100 : pageSize;
		pageSize = pageSize <= 0 ? 10 : pageSize;

		Object companyObj = filter.get("company_id");
		if (!(companyObj instanceof Number)) {
			throw new ResourceException(MSG);
		}
		long companyId = ((Number) companyObj).longValue();

		LambdaQueryWrapper<Monitors> w = new LambdaQueryWrapper<Monitors>()
				.eq(Monitors::getCompanyId, companyId)
				.orderByDesc(Monitors::getMonitorId);

		if (filter.containsKey("wxappid")) {
			w.eq(Monitors::getWxappid, stringifyFilterValue(filter.get("wxappid")));
		}
		if (filter.containsKey("regionauth_id")) {
			w.eq(Monitors::getRegionauthId, stringifyFilterValue(filter.get("regionauth_id")));
		}
		if (filter.containsKey("monitor_path")) {
			w.eq(Monitors::getMonitorPath, stringifyFilterValue(filter.get("monitor_path")));
		}

		Page<Monitors> mp = new Page<>(page, pageSize);
		Page<Monitors> result = monitorsMapper.selectPage(mp, w);
		long total = result.getTotal();
		int totalCount = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;

		List<Map<String, Object>> list = new ArrayList<>();
		for (Monitors m : result.getRecords()) {
			list.add(toListRow(m));
		}

		if (list.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalCount);
			out.put("list", list);
			return out;
		}

		Set<String> regionauthKeys = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			Object rid = row.get("regionauthId");
			if (isFalsyRegionauthId(rid)) {
				continue;
			}
			regionauthKeys.add(String.valueOf(rid));
		}

		Map<String, String> nameById = new LinkedHashMap<>();
		if (!regionauthKeys.isEmpty()) {
			List<Long> longIds = new ArrayList<>();
			for (String sid : regionauthKeys) {
				try {
					longIds.add(Long.parseLong(sid));
				} catch (NumberFormatException ignored) {
				}
			}
			if (!longIds.isEmpty()) {
				List<Regionauth> auths = regionauthMapper.selectList(
						new LambdaQueryWrapper<Regionauth>()
								.eq(Regionauth::getCompanyId, companyId)
								.in(Regionauth::getRegionauthId, longIds));
				for (Regionauth r : auths) {
					if (r.getRegionauthId() != null) {
						String nm = r.getRegionauthName() != null ? r.getRegionauthName() : "";
						nameById.put(String.valueOf(r.getRegionauthId()), nm);
					}
				}
			}
		}

		for (Map<String, Object> row : list) {
			Object rid = row.get("regionauthId");
			String key = rid == null ? "" : String.valueOf(rid);
			row.put("regionauth_name", nameById.getOrDefault(key, ""));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	private static String stringifyFilterValue(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static void validatePage(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("page", List.of("The page field is required."));
			return;
		}
		String s = raw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("page", List.of("The page must be an integer."));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("page", List.of("The page must be an integer."));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("page", List.of("The page must be at least 1."));
			return;
		}
		if (v > Integer.MAX_VALUE) {
			fieldErrors.put("page", List.of("The page must be an integer."));
		}
	}

	private static void validatePageSize(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("pageSize", List.of("The page size field is required."));
			return;
		}
		String s = raw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("pageSize", List.of("The page size must be an integer."));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("pageSize", List.of("The page size must be an integer."));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("pageSize", List.of("The page size must be at least 1."));
			return;
		}
		if (v > 50L) {
			fieldErrors.put("pageSize", List.of("The page size may not be greater than 50."));
		}
	}

	private static int parseValidatedPositiveInt(String trimmed) {
		return (int) Long.parseLong(trimmed);
	}

	private static int parseValidatedPageSizeInt(String trimmed) {
		return Math.toIntExact(Long.parseLong(trimmed));
	}

	private static boolean isFalsyRegionauthId(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b && !b) {
			return true;
		}
		if (o instanceof Number n && n.longValue() == 0L) {
			return true;
		}
		String s = String.valueOf(o);
		if (s.isEmpty()) {
			return true;
		}
		return "0".equals(s);
	}

	private static LinkedHashMap<String, Object> toListRow(Monitors m) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("monitorId", m.getMonitorId());
		row.put("companyId", m.getCompanyId());
		row.put("wxappid", m.getWxappid());
		row.put("nickName", m.getNickName());
		row.put("monitorPath", m.getMonitorPath());
		row.put("monitorPathParams", m.getMonitorPathParams());
		row.put("created", m.getCreated());
		row.put("updated", m.getUpdated());
		row.put("pageName", m.getPageName());
		row.put("regionauthId", m.getRegionauthId());
		row.put("regionauth_name", "");
		return row;
	}
}
