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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorEasyListMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorEasyListFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorEasyListQueryService {

	private final DistributorEasyListMapper distributorEasyListMapper;
	private final DistributorTagRelQueryService distributorTagRelQueryService;
	private final DistributorListQueryService distributorListQueryService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public DistributorEasyListQueryService(
			DistributorEasyListMapper distributorEasyListMapper,
			DistributorTagRelQueryService distributorTagRelQueryService,
			DistributorListQueryService distributorListQueryService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.distributorEasyListMapper = distributorEasyListMapper;
		this.distributorTagRelQueryService = distributorTagRelQueryService;
		this.distributorListQueryService = distributorListQueryService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> buildEasyList(
			HttpServletRequest request, Map<String, Object> user, Map<String, Object> merged) {
		long companyId = longOf(user.get("company_id"));
		String operatorType = stringOf(user.get("operator_type"));
		String requestLang = RequestLangTag.current(langueProperties);

		int page = parsePageIndex(merged.get("page"));
		boolean unlimited = isUnlimitedPageSize(merged.get("pageSize"));
		int pageSize = unlimited ? 0 : parsePageSizeBounded(merged.get("pageSize"), 1000);

		List<Long> distributorScope = resolveInitialDistributorScope(merged);
		List<Long> scopeBeforeTag =
				distributorScope == null ? new ArrayList<>() : new ArrayList<>(distributorScope);
		boolean hadScopeBeforeTag = !scopeBeforeTag.isEmpty();

		List<Long> tagIds = resolveTagIdsForFilter(merged.get("tag_id"));
		if (!tagIds.isEmpty()) {
			List<Long> tagQueryScope = hadScopeBeforeTag ? scopeBeforeTag : null;
			List<Long> byTag =
					distributorTagRelQueryService.listDistributorIdsByCompanyAndTagIds(
							companyId, tagIds, tagQueryScope);
			if (byTag.isEmpty()) {
				return emptyPayload();
			}
			if (hadScopeBeforeTag) {
				distributorScope = mergeUnionPreservingOrder(scopeBeforeTag, byTag);
			} else {
				distributorScope = byTag;
			}
		}

		List<Long> jwtLinkedIds = List.of();
		if ("staff".equals(operatorType) || "distributor".equals(operatorType)) {
			jwtLinkedIds = parseLongIdsFromInput(user.get("distributor_ids"));
		}
		if ("distributor".equals(operatorType) && jwtLinkedIds.isEmpty()) {
			return emptyPayload();
		}
		if (!jwtLinkedIds.isEmpty()) {
			if (distributorScope != null && !distributorScope.isEmpty()) {
				distributorScope = distributorListQueryService.intersectSorted(distributorScope, jwtLinkedIds);
			} else {
				distributorScope = new ArrayList<>(jwtLinkedIds);
			}
		}

		if (distributorScope != null && distributorScope.isEmpty()) {
			return emptyPayload();
		}

		DistributorEasyListFilter f = new DistributorEasyListFilter();
		f.setCompanyId(companyId);
		f.setRequestLang(requestLang);
		if ("merchant".equals(operatorType)) {
			Long mid = longOrNull(user.get("merchant_id"));
			if (mid != null) {
				f.setMerchantIdEq(mid);
			}
		}
		if (distributorScope != null && !distributorScope.isEmpty()) {
			f.setDistributorIdIn(distributorScope);
		}

		Object isValidRaw = merged.get("is_valid");
		if (truthy(isValidRaw)) {
			f.setIsValid(isValidRaw.toString().trim());
		}
		if (truthy(merged.get("name"))) {
			f.setNameContains(stringOf(merged.get("name")).trim());
		}
		if (truthy(merged.get("province"))) {
			f.setProvinceEq(stringOf(merged.get("province")).trim());
		}
		if (truthy(merged.get("city"))) {
			f.setCityEq(stringOf(merged.get("city")).trim());
		}
		if (truthy(merged.get("area"))) {
			f.setAreaEq(stringOf(merged.get("area")).trim());
		}
		if (truthy(merged.get("mobile"))) {
			f.setMobileExact(stringOf(merged.get("mobile")).trim());
		}

		if (unlimited) {
			f.setOffset(0);
			f.setLimit(null);
		} else {
			f.setOffset(Math.max(0, (page - 1) * pageSize));
			f.setLimit(pageSize);
		}

		long total = distributorEasyListMapper.countByFilter(f);
		List<Distributor> entities = distributorEasyListMapper.selectPageByFilter(f);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Distributor d : entities) {
			list.add(toEasyRow(d));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", (int) total);
		return out;
	}

	private static Map<String, Object> emptyPayload() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("total_count", 0);
		return out;
	}

	private List<Long> resolveInitialDistributorScope(Map<String, Object> merged) {
		List<Long> distributorScope = null;
		boolean isAll = truthy(merged.get("is_all"));
		if (!isAll) {
			Object singleDid = merged.get("distributor_id");
			if (truthy(singleDid)) {
				Long id = longOrNull(singleDid);
				if (id != null) {
					distributorScope = new ArrayList<>();
					distributorScope.add(id);
				}
			}
		}
		if (distributorScope == null) {
			List<Long> multi = parseLongIdsFromInput(merged.get("distributorIds"));
			if (!multi.isEmpty()) {
				distributorScope = multi;
			}
		}
		return distributorScope;
	}

	/**
	 * Tag ids for rel query: non-zero ids from JSON/collection; otherwise one id when scalar is truthy (not
	 * {@code "0"} / {@code "false"} / empty).
	 */
	private List<Long> resolveTagIdsForFilter(Object tagRaw) {
		if (tagRaw == null) {
			return List.of();
		}
		if (tagRaw instanceof Collection<?> || (tagRaw instanceof String s && s.trim().startsWith("["))) {
			List<Long> parsed = parseLongIdsFromInput(tagRaw);
			List<Long> out = new ArrayList<>();
			for (Long id : parsed) {
				if (id != null && id != 0L) {
					out.add(id);
				}
			}
			return out;
		}
		if (!truthy(tagRaw)) {
			return List.of();
		}
		Long id = longOrNull(tagRaw);
		if (id == null || id == 0L) {
			return List.of();
		}
		return List.of(id);
	}

	private static List<Long> mergeUnionPreservingOrder(List<Long> a, List<Long> b) {
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		List<Long> out = new ArrayList<>();
		for (Long x : a) {
			if (x != null && seen.add(x)) {
				out.add(x);
			}
		}
		for (Long x : b) {
			if (x != null && seen.add(x)) {
				out.add(x);
			}
		}
		return out;
	}

	private static Map<String, Object> toEasyRow(Distributor d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("address", d.getAddress() != null ? d.getAddress() : "");
		m.put("name", d.getName() != null ? d.getName() : "");
		m.put("shop_code", d.getShopCode() != null ? d.getShopCode() : "");
		m.put("mobile", d.getMobile() != null ? d.getMobile() : "");
		m.put("contact", d.getContact() != null ? d.getContact() : "");
		m.put("distributor_id", d.getDistributorId() != null ? d.getDistributorId() : 0L);
		m.put("logo", d.getLogo());
		m.put("hour", d.getHour() != null ? d.getHour() : "");
		long parent = d.getShopId() != null ? d.getShopId() : 0L;
		m.put("parent_distributor_id", parent);
		m.put("lng", d.getLng() != null ? d.getLng() : "");
		m.put("lat", d.getLat() != null ? d.getLat() : "");
		boolean isDist = d.getIsDistributor() == null || Boolean.TRUE.equals(d.getIsDistributor());
		m.put("is_distributor", isDist);
		int def = d.getIsDefault() != null ? d.getIsDefault() : 0;
		m.put("is_default", def != 0);
		return m;
	}

	private static boolean isUnlimitedPageSize(Object raw) {
		Long n = longOrNull(raw);
		return n != null && n <= 0;
	}

	private static int parsePageIndex(Object v) {
		Long n = longOrNull(v);
		if (n == null || n < 1) {
			return 1;
		}
		if (n > Integer.MAX_VALUE) {
			return 1;
		}
		return n.intValue();
	}

	private static int parsePageSizeBounded(Object v, int def) {
		Long n = longOrNull(v);
		if (n == null || n < 1) {
			return def;
		}
		if (n > Integer.MAX_VALUE) {
			return def;
		}
		return n.intValue();
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private List<Long> parseLongIdsFromInput(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				if (o instanceof Map<?, ?> mm) {
					Long v = longOrNull(mm.get("distributor_id"));
					if (v != null) {
						out.add(v);
					}
				} else {
					Long v = longOrNull(o);
					if (v != null) {
						out.add(v);
					}
				}
			}
			return out;
		}
		if (raw instanceof String s && s.trim().startsWith("[")) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node.isArray()) {
					List<Long> out = new ArrayList<>();
					for (JsonNode n : node) {
						if (n.isObject()) {
							JsonNode idNode = n.get("distributor_id");
							if (idNode != null && idNode.isNumber()) {
								out.add(idNode.longValue());
							} else if (idNode != null && idNode.isTextual()) {
								Long v = longOrNull(idNode.asText());
								if (v != null) {
									out.add(v);
								}
							}
						} else if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual()) {
							Long v = longOrNull(n.asText());
							if (v != null) {
								out.add(v);
							}
						}
					}
					return out;
				}
			} catch (Exception ignored) {
				return List.of();
			}
		}
		Long single = longOrNull(raw);
		if (single != null) {
			return List.of(single);
		}
		return List.of();
	}

	private static long longOf(Object o) {
		Long v = longOrNull(o);
		return v != null ? v : 0L;
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringOf(Object o) {
		return o == null ? "" : o.toString();
	}
}
