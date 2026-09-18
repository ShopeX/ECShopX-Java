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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.dto.DistributorTagRelRow;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorAdminListCoreService {

	/**
	 * PHP {@code DistributorCloudShopStatusGuard::isValidValuesForCloudAllListFilter}：
	 * 启用/禁用/闭店 + 历史 1/0，不含撤店 {@code delete}。
	 */
	private static final List<String> IS_VALID_CLOUD_ALL =
			List.of("true", "false", "closed", "1", "0");

	private final DistributorAdminListRepository distributorAdminListRepository;
	private final DistributorTagRelQueryService distributorTagRelQueryService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final DistributorCategoryService distributorCategoryService;
	private final ObjectMapper objectMapper;

	public DistributorAdminListCoreService(
			DistributorAdminListRepository distributorAdminListRepository,
			DistributorTagRelQueryService distributorTagRelQueryService,
			DistributorSelfMetaService distributorSelfMetaService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			DistributorListQueryService distributorListQueryService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			DistributorCategoryService distributorCategoryService,
			ObjectMapper objectMapper) {
		this.distributorAdminListRepository = distributorAdminListRepository;
		this.distributorTagRelQueryService = distributorTagRelQueryService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.distributorCategoryService = distributorCategoryService;
		this.objectMapper = objectMapper;
	}

	public DistributorAdminListResult buildCore(DistributorAdminListContext ctx) {
		Map<String, Object> in = ctx.getMergedInput();
		Map<String, Object> jwt = ctx.getJwt();
		long companyId = longOf(jwt.get("company_id"));
		String operatorType = stringOf(jwt.get("operator_type"));
		boolean storesOnly = ctx.isStoresOnlyMode();

		int page = parsePositiveInt(in.get("page"), 1);
		int pageSize = parsePositiveInt(in.get("pageSize"), 1000);

		DistributorAdminListFilter f = new DistributorAdminListFilter();
		f.setCompanyId(companyId);
		if (storesOnly) {
			f.setIsDistributorEq(Boolean.FALSE);
			f.setDistributorSelfEq(null);
		} else if (!showDistributorSelfEnabled(in.get("show_distributor_self"))) {
			// PHP：默认不展示虚拟门店；show_distributor_self=1/true 时不过滤 distributor_self
			f.setDistributorSelfEq(0);
		}
		f.setRequestLang(
				StringUtils.hasText(ctx.getRequestLang()) ? ctx.getRequestLang() : "zh-CN");
		if (!storesOnly && "merchant".equals(operatorType)) {
			Long mid = longOrNull(jwt.get("merchant_id"));
			if (mid != null) {
				f.setMerchantIdEq(mid);
			}
		}
		if (!storesOnly && truthyUnbound(in.get("unbound"))) {
			f.setUnbound(Boolean.TRUE);
		}
		String name = stringOf(in.get("name"));
		if (StringUtils.hasText(name)) {
			f.setNameContains(name.trim());
		}
		if (!storesOnly) {
			String shopCode = stringOf(in.get("shop_code"));
			if (StringUtils.hasText(shopCode)) {
				f.setShopCodeContains(shopCode.trim());
			}
			Integer openDiv = parseOpenDivided(in.get("open_divided"));
			if (openDiv != null) {
				f.setOpenDivided(openDiv);
			}
		}
		if (storesOnly) {
			String province = stringOf(in.get("province"));
			if (StringUtils.hasText(province)) {
				f.setProvinceEq(province.trim());
			}
			String city = stringOf(in.get("city"));
			if (StringUtils.hasText(city)) {
				f.setCityEq(city.trim());
			}
			Object areaRaw = in.get("area");
			if (areaRaw != null && StringUtils.hasText(areaRaw.toString())) {
				f.setAreaEq(areaRaw.toString().trim());
			}
		} else {
			String province = stringOf(in.get("province"));
			if (StringUtils.hasText(province)) {
				f.setProvinceContains(province.trim());
			}
			String city = stringOf(in.get("city"));
			if (StringUtils.hasText(city)) {
				f.setCityContains(city.trim());
			}
			Object areaRaw = in.get("area");
			if (areaRaw != null && StringUtils.hasText(areaRaw.toString())) {
				f.setAreaContains(areaRaw.toString().trim().replace("区", ""));
			}
		}
		String mobile = stringOf(in.get("mobile"));
		if (StringUtils.hasText(mobile)) {
			f.setMobileExact(mobile.trim());
		}
		if (!storesOnly) {
			String merchantName = stringOf(in.get("merchant_name"));
			if (StringUtils.hasText(merchantName)) {
				f.setMerchantNameLike(merchantName.trim());
			}
		}
		if (!storesOnly) {
			Object distType = in.get("distribution_type");
			if (distType != null && StringUtils.hasText(distType.toString())) {
				Long dt = longOrNull(distType);
				if (dt != null) {
					f.setDistributionType(dt.intValue());
				}
			}
			if ("merchant".equals(operatorType) || "distributor".equals(operatorType)) {
				f.setDistributionType(null);
			}
		}
		if (!storesOnly) {
			Integer pay = parsePaymentSubject(in.get("payment_subject"));
			if (pay != null) {
				f.setPaymentSubject(pay);
			}
		}
		if (!storesOnly && truthy(in.get("distributor_category_id"))) {
			Long catId = longOrNull(in.get("distributor_category_id"));
			if (catId != null) {
				f.setDistributorCategoryIdEq(catId);
			}
		}
		Object isValid = in.get("is_valid");
		if (isValid != null && StringUtils.hasText(isValid.toString())) {
			String iv = isValid.toString().trim();
			if ("cloud_all".equalsIgnoreCase(iv)) {
				f.setIsValidIn(IS_VALID_CLOUD_ALL);
			} else {
				f.setIsValid(iv);
			}
		}

		List<Long> distributorScope = null;
		boolean distributorFilterSet = false;
		boolean isAll = truthy(in.get("is_all"));
		if (!isAll) {
			List<Long> fromDid = parseLongIdsFromInput(in.get("distributor_id"));
			if (!fromDid.isEmpty()) {
				distributorScope = new ArrayList<>(fromDid);
				distributorFilterSet = true;
			}
		}
		if (distributorScope == null) {
			List<Long> multi = parseLongIdsFromInput(in.get("distributorIds"));
			if (!multi.isEmpty()) {
				distributorScope = multi;
				distributorFilterSet = true;
			}
		}

		List<Long> tagIds = parseLongIdsFromInput(in.get("tag_id"));
		if (!tagIds.isEmpty()) {
			List<Long> byTag = distributorTagRelQueryService.listDistributorIdsByCompanyAndTagIds(
					companyId, tagIds, distributorScope);
			if (byTag.isEmpty()) {
				DistributorAdminListResult empty = new DistributorAdminListResult();
				empty.setList(List.of());
				empty.setTotalCount(0);
				empty.setTopTagList(List.of());
				empty.setDistributorSelf(distributorSelfMetaService.getDistributorSelf(companyId));
				return empty;
			}
			distributorScope = byTag;
			distributorFilterSet = true;
		}

		List<Long> jwtLinkedDistributorIds = List.of();
		if (!storesOnly && "staff".equals(operatorType)) {
			jwtLinkedDistributorIds =
					parseLinkedDistributorIdsFromJwtClaim(jwt.get("distributor_ids"), objectMapper);
		} else if (!storesOnly && "distributor".equals(operatorType)) {
			jwtLinkedDistributorIds =
					parseLinkedDistributorIdsFromJwtClaim(jwt.get("distributor_ids"), objectMapper);
			if (jwtLinkedDistributorIds.isEmpty()) {
				DistributorAdminListResult empty = new DistributorAdminListResult();
				empty.setList(List.of());
				empty.setTotalCount(0);
				empty.setTopTagList(List.of());
				empty.setDistributorSelf(distributorSelfMetaService.getDistributorSelf(companyId));
				return empty;
			}
		}
		if (!jwtLinkedDistributorIds.isEmpty()) {
			if (distributorScope != null && !distributorScope.isEmpty()) {
				distributorScope =
						distributorListQueryService.intersectSorted(distributorScope, jwtLinkedDistributorIds);
				distributorFilterSet = true;
			} else {
				distributorScope = new ArrayList<>(jwtLinkedDistributorIds);
				distributorFilterSet = true;
			}
		}

		if (distributorScope != null && distributorScope.isEmpty()) {
			DistributorAdminListResult empty = new DistributorAdminListResult();
			empty.setList(List.of());
			empty.setTotalCount(0);
			empty.setTopTagList(List.of());
			empty.setDistributorSelf(distributorSelfMetaService.getDistributorSelf(companyId));
			return empty;
		}

		if (distributorScope != null && !distributorScope.isEmpty()) {
			f.setDistributorIdIn(distributorScope);
		}

		f.setOffset(Math.max(0, (page - 1) * pageSize));
		f.setLimit(pageSize);

		long total = distributorAdminListRepository.countByFilter(f);
		List<Distributor> entities = distributorAdminListRepository.selectPageByFilter(f);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (Distributor d : entities) {
			long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
			Map<String, Object> setting =
					selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> formatted =
					distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			rows.add(formatted);
		}

		distributorListOutsideLangReadService.applyLangMaps(companyId, f.getRequestLang(), rows);
		distributorCategoryService.appendDistributorCategoryName(companyId, rows);

		List<Long> pageIds = new ArrayList<>();
		for (Distributor d : entities) {
			if (d.getDistributorId() != null) {
				pageIds.add(d.getDistributorId());
			}
		}
		List<DistributorTagRelRow> relRows =
				distributorTagRelQueryService.listRelWithTagsByCompanyAndDistributorIds(companyId, pageIds);
		Map<Long, List<Map<String, Object>>> tagsByDistributor = new LinkedHashMap<>();
		Map<Long, Map<String, Object>> topTags = new LinkedHashMap<>();
		for (DistributorTagRelRow rr : relRows) {
			Map<String, Object> tagRow = relRowToMap(rr);
			tagsByDistributor
					.computeIfAbsent(rr.getDistributorId(), k -> new ArrayList<>())
					.add(tagRow);
			topTags.putIfAbsent(rr.getTagId(), tagRow);
		}
		for (Map<String, Object> row : rows) {
			Object didObj = row.get("distributor_id");
			long did = didObj instanceof Number n ? n.longValue() : longOf(didObj);
			row.put("tagList", tagsByDistributor.getOrDefault(did, List.of()));
		}

		DistributorAdminListResult result = new DistributorAdminListResult();
		result.setList(rows);
		result.setTotalCount((int) total);
		result.setTopTagList(new ArrayList<>(topTags.values()));
		result.setDistributorSelf(distributorSelfMetaService.getDistributorSelf(companyId));

		String productModel = ctx.getProductModel() != null ? ctx.getProductModel() : "";
		int isApp = parsePositiveInt(in.get("is_app"), 0);
		if (!storesOnly && !"platform".equals(productModel) && isApp != 0) {
			for (Map<String, Object> row : result.getList()) {
				row.put("is_center", Boolean.FALSE);
			}
			boolean skipCenterInsert = distributorFilterSet
					&& distributorScope != null
					&& !distributorScope.isEmpty()
					&& !distributorScope.contains(0L);
			if (skipCenterInsert) {
				normalizeOpenDivided(result.getList());
				return result;
			}
			if (page == 1) {
				Map<String, Object> selfInfo = distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId);
				selfInfo.put("is_center", Boolean.TRUE);
				result.getList().add(0, selfInfo);
				result.setTotalCount(result.getTotalCount() + 1);
			}
		}

		normalizeOpenDivided(result.getList());
		return result;
	}

	private static Map<String, Object> relRowToMap(DistributorTagRelRow rr) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_id", rr.getDistributorId() != null ? rr.getDistributorId().toString() : "");
		m.put("tag_id", rr.getTagId() != null ? rr.getTagId().toString() : "");
		m.put("company_id", rr.getCompanyId() != null ? rr.getCompanyId().toString() : "");
		m.put("tag_name", rr.getTagName());
		m.put("tag_color", rr.getTagColor());
		m.put("font_color", rr.getFontColor());
		m.put("description", rr.getDescription());
		m.put("tag_icon", rr.getTagIcon());
		m.put("front_show", rr.getFrontShow() != null ? rr.getFrontShow().toString() : "");
		m.put("created", rr.getCreated() != null ? rr.getCreated().toString() : "");
		m.put("updated", rr.getUpdated() != null ? rr.getUpdated().toString() : "");
		m.put("merchant_name", "");
		// Tag-rel rows may not carry merchant fields; keep the same key shape as full list rows.
		m.put("distribution_type", "");
		return m;
	}

	private static void normalizeOpenDivided(List<Map<String, Object>> list) {
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> row = list.get(i);
			Object v = row.get("open_divided");
			boolean b = false;
			if (v instanceof Number n) {
				b = n.intValue() == 1;
			} else if (v instanceof Boolean bool) {
				b = bool;
			} else if (v != null) {
				b = "1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString());
			}
			row.put("open_divided", b);
		}
	}

	/**
	 * PHP swagger：{@code show_distributor_self} 传 1 或 true 时列表不过滤虚拟门店。
	 */
	private static boolean showDistributorSelfEnabled(Object v) {
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
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean truthyUnbound(Object v) {
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

	private static boolean truthy(Object v) {
		return truthyUnbound(v);
	}

	private static Integer parseOpenDivided(Object v) {
		if (v == null) {
			return null;
		}
		if (Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(stringOf(v))) {
			return 1;
		}
		if (Boolean.FALSE.equals(v) || "false".equalsIgnoreCase(stringOf(v))) {
			return 0;
		}
		Long n = longOrNull(v);
		if (n != null) {
			return n.intValue() != 0 ? 1 : 0;
		}
		return null;
	}

	private static Integer parsePaymentSubject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			int i = n.intValue();
			if (i == 0 || i == 1) {
				return i;
			}
			return null;
		}
		String s = v.toString().trim();
		if ("0".equals(s) || "1".equals(s)) {
			return Integer.parseInt(s);
		}
		return null;
	}

	private static int parsePositiveInt(Object v, int def) {
		Long n = longOrNull(v);
		if (n == null || n < 1) {
			return def;
		}
		if (n > Integer.MAX_VALUE) {
			return def;
		}
		return n.intValue();
	}

	/**
	 * PHP {@code distributor_ids} / middleware {@code distributorIds}: list of {@code {distributor_id}}, JSON string, or numeric ids.
	 */
	private static List<Long> parseLinkedDistributorIdsFromJwtClaim(Object raw, ObjectMapper objectMapper) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "null".equalsIgnoreCase(t)) {
				return List.of();
			}
			if (t.startsWith("[")) {
				try {
					JsonNode node = objectMapper.readTree(t);
					if (node != null && node.isArray()) {
						return collectDistributorIdsFromJsonArray(node);
					}
				} catch (Exception ignored) {
					return List.of();
				}
			}
			Long single = longOrNull(t);
			return single != null ? List.of(single) : List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> fromNested = new ArrayList<>();
			for (Object row : c) {
				if (row instanceof Map<?, ?> m) {
					Long v = longOrNull(m.get("distributor_id"));
					if (v != null) {
						fromNested.add(v);
					}
				} else if (row instanceof Number n) {
					fromNested.add(n.longValue());
				} else if (row instanceof String str) {
					Long v = longOrNull(str);
					if (v != null) {
						fromNested.add(v);
					}
				}
			}
			if (!fromNested.isEmpty()) {
				return dedupePreserveOrder(fromNested);
			}
		}
		return List.of();
	}

	private static List<Long> collectDistributorIdsFromJsonArray(JsonNode node) {
		List<Long> out = new ArrayList<>();
		for (JsonNode n : node) {
			if (n == null || n.isNull()) {
				continue;
			}
			if (n.isObject() && n.hasNonNull("distributor_id")) {
				Long v = longOrNull(n.get("distributor_id"));
				if (v != null) {
					out.add(v);
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
		return dedupePreserveOrder(out);
	}

	private static List<Long> dedupePreserveOrder(List<Long> in) {
		if (in.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		for (Long x : in) {
			if (x != null) {
				seen.add(x);
			}
		}
		return seen.isEmpty() ? List.of() : new ArrayList<>(seen);
	}

	private List<Long> parseLongIdsFromInput(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				Long v = longOrNull(o);
				if (v != null) {
					out.add(v);
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
						if (n.isNumber()) {
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
