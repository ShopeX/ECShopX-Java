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

package cn.shopex.ecshopx.shopmenuborder.service;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopMenuExportService {

	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final ShopMenuService shopMenuService;
	private final LangueProperties langueProperties;
	private final ShopMenuOutsideLangReadService shopMenuOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public ShopMenuExportService(
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			ShopMenuService shopMenuService,
			LangueProperties langueProperties,
			ShopMenuOutsideLangReadService shopMenuOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.shopMenuService = shopMenuService;
		this.langueProperties = langueProperties;
		this.shopMenuOutsideLangReadService = shopMenuOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, String> exportDownShopMenu(HttpServletRequest request) throws JsonProcessingException {
		if (hasNonNumericNonZeroVersionParameter(request)) {
			return buildExportForFlatList(new ArrayList<>());
		}

		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.eq(ShopMenu::getDisabled, false)
				.eq(ShopMenu::getIsShow, true)
				.orderByAsc(ShopMenu::getPid, ShopMenu::getSort)
				.last("LIMIT 1000");
		resolveExportVersionFilter(request).ifPresent(v -> w.eq(ShopMenu::getVersion, v));

		List<ShopMenu> entities = shopMenuMapper.selectList(w);
		List<Map<String, Object>> rowMaps = new ArrayList<>();
		if (entities != null) {
			for (ShopMenu sm : entities) {
				rowMaps.add(ShopMenuQueryService.toSnakeRow(sm));
			}
		}

		Map<Long, List<String>> menuRelIndex = loadMenuRelIndexForCompanyZero();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> flat =
				(List<Map<String, Object>>)
						ShopMenuQueryService.buildTreeAndFlatListFromSnakeRows(rowMaps, menuRelIndex)
								.get("list");

		return buildExportForFlatList(flat);
	}

	private Map<Long, List<String>> loadMenuRelIndexForCompanyZero() {
		LambdaQueryWrapper<ShopMenuRelType> rw = new LambdaQueryWrapper<>();
		rw.eq(ShopMenuRelType::getCompanyId, 0);
		List<ShopMenuRelType> relRows = shopMenuRelTypeMapper.selectList(rw);
		Map<Long, List<String>> menuRelIndex = new LinkedHashMap<>();
		if (relRows != null) {
			for (ShopMenuRelType rel : relRows) {
				if (rel.getShopmenuId() == null) {
					continue;
				}
				String name = shopMenuService.menuTypeIdToName(rel.getMenuType());
				menuRelIndex.computeIfAbsent(rel.getShopmenuId(), k -> new ArrayList<>()).add(name);
			}
		}
		return menuRelIndex;
	}

	private void enrichListForExport(List<Map<String, Object>> flat) {
		for (Map<String, Object> item : flat) {
			item.put("name_lang", new LinkedHashMap<String, Object>());
		}
		if (flat.isEmpty()) {
			return;
		}

		Set<Long> ids = new LinkedHashSet<>();
		for (Map<String, Object> item : flat) {
			long sid = ShopMenuQueryService.longVal(item.get("shopmenu_id"));
			if (sid > 0L) {
				ids.add(sid);
			}
		}
		Map<Long, String> mainNames = loadMainTableNames(ids);
		for (Map<String, Object> item : flat) {
			long sid = ShopMenuQueryService.longVal(item.get("shopmenu_id"));
			String fromMain = mainNames.get(sid);
			Object prev = item.get("name");
			String fallback = prev != null ? prev.toString() : "";
			item.put("name", fromMain != null ? fromMain : fallback);
		}

		List<String> langs = langueProperties.getList();
		if (langs == null) {
			return;
		}
		Map<Long, Set<Long>> byCompany = new LinkedHashMap<>();
		for (Map<String, Object> item : flat) {
			long cid = ShopMenuQueryService.longVal(item.get("company_id"));
			long sid = ShopMenuQueryService.longVal(item.get("shopmenu_id"));
			byCompany.computeIfAbsent(cid, k -> new LinkedHashSet<>()).add(sid);
		}
		for (String langue : langs) {
			if (langue == null || !StringUtils.hasText(langue.trim())) {
				continue;
			}
			String langTag = langue.trim();
			Map<Long, Map<Long, String>> perCompany = new LinkedHashMap<>();
			for (Map.Entry<Long, Set<Long>> e : byCompany.entrySet()) {
				if (e.getValue().isEmpty()) {
					continue;
				}
				Map<Long, String> m =
						shopMenuOutsideLangReadService.findShopMenuNamesByLocale(
								e.getKey(), e.getValue(), langTag);
				perCompany.put(e.getKey(), m);
			}
			for (Map<String, Object> item : flat) {
				long cid = ShopMenuQueryService.longVal(item.get("company_id"));
				long sid = ShopMenuQueryService.longVal(item.get("shopmenu_id"));
				@SuppressWarnings("unchecked")
				Map<String, Object> nl = (Map<String, Object>) item.get("name_lang");
				String v = perCompany.getOrDefault(cid, Map.of()).getOrDefault(sid, "");
				nl.put(langTag, v);
			}
		}
	}

	private Map<Long, String> loadMainTableNames(Collection<Long> shopmenuIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (shopmenuIds == null || shopmenuIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<ShopMenu> w = new LambdaQueryWrapper<>();
		w.in(ShopMenu::getShopmenuId, shopmenuIds);
		List<ShopMenu> rows = shopMenuMapper.selectList(w);
		if (rows == null) {
			return out;
		}
		for (ShopMenu sm : rows) {
			if (sm.getShopmenuId() == null) {
				continue;
			}
			out.put(sm.getShopmenuId(), sm.getName() != null ? sm.getName() : "");
		}
		return out;
	}

	/**
	 * Export path only: reads the {@code version} request parameter. When it is a non-blank integer other than
	 * {@code 0}, the export query is restricted to that menu version; otherwise no {@code version} predicate is
	 * applied. A present but non-numeric value (other than blank or {@code 0}) is handled in
	 * {@link #exportDownShopMenu} by returning an empty export.
	 */
	private static Optional<Integer> resolveExportVersionFilter(HttpServletRequest request) {
		String raw = request.getParameter("version");
		if (raw == null) {
			return Optional.empty();
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Integer.parseInt(t));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	/**
	 * True when {@code version} is present, non-blank after trim, not {@code 0}, and not parseable as an int — same
	 * cases where a legacy export would apply an impossible filter and yield no rows.
	 */
	private static boolean hasNonNumericNonZeroVersionParameter(HttpServletRequest request) {
		String raw = request.getParameter("version");
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return false;
		}
		try {
			Integer.parseInt(t);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private Map<String, String> buildExportForFlatList(List<Map<String, Object>> flat)
			throws JsonProcessingException {
		enrichListForExport(flat);
		String json = objectMapper.writeValueAsString(flat);
		String filename =
				"菜单-"
						+ LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)
						+ ".json";
		String file =
				"data:text/plain;base64,"
						+ Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Map<String, String> out = new LinkedHashMap<>();
		out.put("name", filename);
		out.put("file", file);
		return out;
	}
}
