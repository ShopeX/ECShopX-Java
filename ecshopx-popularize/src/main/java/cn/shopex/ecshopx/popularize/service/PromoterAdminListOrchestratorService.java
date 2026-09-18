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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("promoterAdminListOrchestratorService")
public class PromoterAdminListOrchestratorService {

	private final MerchantScopeDistributorIdsReadService merchantScopeDistributorIdsReadService;
	private final ShopSalespersonPromoterListSupportService shopSalespersonPromoterListSupportService;
	private final PromoterListQueryService promoterListQueryService;
	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;
	private final PopularizeBrokerageCountReadService popularizeBrokerageCountReadService;
	private final MemberAccountService memberAccountService;
	private final PromoterListMemberWideMergeReadService promoterListMemberWideMergeReadService;
	private final PromoterIdentityMapper promoterIdentityMapper;

	public PromoterAdminListOrchestratorService(
			MerchantScopeDistributorIdsReadService merchantScopeDistributorIdsReadService,
			ShopSalespersonPromoterListSupportService shopSalespersonPromoterListSupportService,
			PromoterListQueryService promoterListQueryService,
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService,
			PopularizeBrokerageCountReadService popularizeBrokerageCountReadService,
			MemberAccountService memberAccountService,
			PromoterListMemberWideMergeReadService promoterListMemberWideMergeReadService,
			PromoterIdentityMapper promoterIdentityMapper) {
		this.merchantScopeDistributorIdsReadService = merchantScopeDistributorIdsReadService;
		this.shopSalespersonPromoterListSupportService = shopSalespersonPromoterListSupportService;
		this.promoterListQueryService = promoterListQueryService;
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
		this.popularizeBrokerageCountReadService = popularizeBrokerageCountReadService;
		this.memberAccountService = memberAccountService;
		this.promoterListMemberWideMergeReadService = promoterListMemberWideMergeReadService;
		this.promoterIdentityMapper = promoterIdentityMapper;
	}

	public Map<String, Object> getPromoterList(
			long companyId,
			long merchantId,
			String operatorType,
			int page,
			int pageSize,
			String mobile,
			String username,
			String identityName,
			String storeStatusRaw,
			String timeStartBegin,
			String timeStartEnd,
			String distributorIdRaw,
			boolean isAll,
			String distributorIdsRaw,
			String pathSourceParam) {
		String pathSourceNorm = normalizePathSourceForSubstringSearch(pathSourceParam);
		boolean pathHasSellers = pathSourceNorm.contains("sellers");
		boolean pathHasPopularizedata = pathSourceNorm.contains("popularizedata");
		boolean pathHasShopadmin = pathSourceNorm.contains("shopadmin");

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();

		boolean hadMobileQuery =
				mobile != null && !mobile.trim().isEmpty();
		boolean hadUsernameQuery =
				username != null && !username.trim().isEmpty();
		boolean hadIdentityNameQuery =
				identityName != null && !identityName.trim().isEmpty();

		if (hadMobileQuery) {
			filter.put("mobile", mobile.trim());
		}
		if (hadUsernameQuery) {
			filter.put("username", username.trim());
		}
		if (hadIdentityNameQuery) {
			filter.put("identity_name", identityName.trim());
		}

		if (storeStatusRaw != null && !storeStatusRaw.trim().isEmpty()) {
			filter.put("shop_status", storeStatusRaw.trim());
		}
		if (timeStartBegin != null && !timeStartBegin.trim().isEmpty()) {
			filter.put("created|>=", timeStartBegin.trim());
			filter.put("created|<=", timeStartEnd == null ? "" : timeStartEnd.trim());
		}

		LinkedHashMap<String, Object> filterUser = new LinkedHashMap<>();

		String distributorIdTrimmed = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		long distributorIdLeading = LeadingNumberParser.parseAsLong(distributorIdTrimmed);
		if (!isAll && distributorIdLeading > 0L) {
			filterUser.put("shop_id", List.of(distributorIdLeading));
		} else if (distributorIdsRaw != null && !distributorIdsRaw.trim().isEmpty()) {
			String[] parts = distributorIdsRaw.split(",");
			List<Long> tokenLongs = new ArrayList<>();
			for (String part : parts) {
				String seg = part == null ? "" : part.trim();
				if (seg.isEmpty()) {
					continue;
				}
				long v = LeadingNumberParser.parseAsLong(seg);
				if (v > 0L) {
					tokenLongs.add(v);
				}
			}
			if (!tokenLongs.isEmpty()) {
				filterUser.put("shop_id", tokenLongs);
			}
		}

		List<Long> shopIds = null;
		if ("merchant".equalsIgnoreCase(operatorType)) {
			shopIds = merchantScopeDistributorIdsReadService.listDistributorIdsForMerchantShops(companyId, merchantId);
			filterUser.put("shop_id", shopIds);
		}

		boolean hasShopFilter =
				filterUser.containsKey("shop_id") && isNonEmptyShopIdValue(filterUser.get("shop_id"));
		boolean gate = hasShopFilter || pathHasSellers || "merchant".equalsIgnoreCase(operatorType);
		if (gate) {
			filterUser.put("company_id", companyId);
			List<Long> shopIdsArg = null;
			Object shopRaw = filterUser.get("shop_id");
			if (shopRaw instanceof List<?> && isNonEmptyShopIdValue(shopRaw)) {
				shopIdsArg = normalizeShopIdListForGate(shopRaw);
			}
			List<Long> userIdsFromSp =
					shopSalespersonPromoterListSupportService.listUserIdsForSalespersonGate(companyId, shopIdsArg);
			boolean distributorOnlySingleShopNarrow =
					isDistributorIdSingleShopNarrowOnly(
							operatorType, isAll, distributorIdLeading, shopRaw);
			if (!userIdsFromSp.isEmpty()) {
				filter.put("user_id", userIdsFromSp);
			} else if (!distributorOnlySingleShopNarrow) {
				filter.put("user_id", List.of(-1L));
			}
		}

		filter.put("company_id", companyId);

		applyPromoterListCallerFilterSameAsListQueryService(companyId, filter);

		Map<String, Object> data = promoterListQueryService.getPromoterList(filter, page, pageSize);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> listRows = (List<Map<String, Object>>) data.get("list");
		if (listRows == null) {
			listRows = List.of();
		}
		LinkedHashMap<Long, String> promoterListRowUpdatedSerializedByUserId = new LinkedHashMap<>();
		listRows = mergeMemberWideFieldsIntoPromoterRows(companyId, listRows, promoterListRowUpdatedSerializedByUserId);
		long totalCount = longFrom(data.get("total_count"));

		List<Long> pageUserIds = new ArrayList<>();
		for (Map<String, Object> row : listRows) {
			if (row == null) {
				continue;
			}
			Object uo = row.get("user_id");
			if (uo == null) {
				continue;
			}
			long uid = longFrom(uo);
			if (uid > 0L) {
				pageUserIds.add(uid);
			}
		}

		if (!pageUserIds.isEmpty()) {
			List<Long> shopIdsForName = "merchant".equalsIgnoreCase(operatorType) ? shopIds : null;
			Map<Long, String> salespersonNameByUser =
					shopSalespersonPromoterListSupportService.mapUserIdToSalespersonName(
							companyId, pageUserIds, shopIdsForName);
			for (Map<String, Object> rowP : listRows) {
				if (rowP == null) {
					continue;
				}
				long uidRow = longFrom(rowP.get("user_id"));
				String nameSale = salespersonNameByUser.getOrDefault(uidRow, "");
				rowP.put("nameSalePerson", nameSale);
				if (StringUtils.hasText(nameSale)) {
					Object un = rowP.get("username");
					String base = un != null ? String.valueOf(un) : "";
					rowP.put("username", base + " ( 业务员：" + nameSale + " ) ");
				}
			}
		}
		for (Map<String, Object> rowP : listRows) {
			if (rowP == null) {
				continue;
			}
			if (!rowP.containsKey("nameSalePerson")) {
				rowP.put("nameSalePerson", "");
			}
		}

		List<Map<String, Object>> countDataShopList = new ArrayList<>();
		Map<Long, Map<String, Object>> countDataShopListUser = new LinkedHashMap<>();

		boolean runBrokerageAggregation =
				distributorIdQueryTruthyForBrokerage(distributorIdRaw) || pathHasPopularizedata;
		if (runBrokerageAggregation) {
			LinkedHashMap<String, Object> brokerageParams = new LinkedHashMap<>();
			brokerageParams.put("company_id", companyId);
			long distributorIdForBrokerage = LeadingNumberParser.parseAsLong(distributorIdTrimmed);
			if (distributorIdForBrokerage > 0L) {
				brokerageParams.put("distributor_id", distributorIdForBrokerage);
			}
			if (filter.containsKey("user_id")) {
				brokerageParams.put("user_id", filter.get("user_id"));
			}
			if ("merchant".equalsIgnoreCase(operatorType) && shopIds != null && !shopIds.isEmpty()) {
				brokerageParams.put("dIds", shopIds);
			}
			brokerageParams.put("groupby", "distributor_id");

			countDataShopList =
					salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountList(
							brokerageParams, pageSize, page);
			for (Map<String, Object> row : countDataShopList) {
				if (row == null) {
					continue;
				}
				Object uo = row.get("user_id");
				if (uo == null) {
					continue;
				}
				long uid = longFrom(uo);
				countDataShopListUser.put(uid, row);
			}
		}

		boolean earlyReturnBrokerageRowsOnly =
				pathHasPopularizedata && (pathHasSellers || pathHasShopadmin);
		if (earlyReturnBrokerageRowsOnly) {
			return Map.of("responseShape", "LIST_BROKERAGE_ROWS", "body", countDataShopList);
		}

		LinkedHashMap<String, Object> filterSnapshotMap =
				buildFilterSnapshotMap(
						companyId,
						filter,
						storeStatusRaw,
						timeStartBegin,
						timeStartEnd,
						hadIdentityNameQuery);

		LinkedHashMap<String, Object> bodyMap = new LinkedHashMap<>();
		bodyMap.put("total_count", totalCount);
		bodyMap.put("list", listRows);
		bodyMap.put("filter", filterSnapshotMap);
		bodyMap.put("countDataShopList", countDataShopList == null ? List.of() : countDataShopList);
		bodyMap.put("datapass_block", 0);

		if (totalCount > 0L) {
			List<Long> distinctPageUids =
					pageUserIds.stream().filter(id -> id > 0L).distinct().toList();
			Map<Long, Map<String, Object>> indexByUser =
					popularizeBrokerageCountReadService.batchPromoterBrokerageCountsByUserIds(
							companyId, distinctPageUids);
			for (Map<String, Object> row : listRows) {
				if (row == null) {
					continue;
				}
				long uid = longFrom(row.get("user_id"));
				LinkedHashMap<String, Object> mergedStats = new LinkedHashMap<>(defaultBrokerageStatsNine());
				mergedStats.putAll(indexByUser.getOrDefault(uid, Map.of()));
				row.putAll(mergedStats);
				Map<String, Object> shopRow = countDataShopListUser.get(uid);
				if (shopRow != null) {
					row.put("shopdata_price_sum", shopRow.getOrDefault("price_sum", 0L));
					row.put("shopdata_rebate_sum", shopRow.getOrDefault("rebate_sum", 0L));
					row.put("shopdata_rebate_sum_noclose", shopRow.getOrDefault("rebate_sum_noclose", 0L));
					row.put("shopdata_total_fee", shopRow.getOrDefault("total_fee", 0L));
				} else {
					row.put("shopdata_price_sum", 0L);
					row.put("shopdata_rebate_sum", 0L);
					row.put("shopdata_rebate_sum_noclose", 0L);
					row.put("shopdata_total_fee", 0L);
				}
			}
		}

		PromoterAdminListPromoterRowCompleter.reapplyListRowUpdatedFromSidecar(
				listRows, promoterListRowUpdatedSerializedByUserId);

		return Map.of("responseShape", "OBJECT_WRAPPER", "body", bodyMap);
	}

	private List<Map<String, Object>> mergeMemberWideFieldsIntoPromoterRows(
			long companyId,
			List<Map<String, Object>> listRows,
			LinkedHashMap<Long, String> promoterListRowUpdatedSerializedByUserId) {
		if (listRows == null || listRows.isEmpty()) {
			return listRows == null ? List.of() : listRows;
		}
		List<Long> uids = new ArrayList<>();
		for (Map<String, Object> row : listRows) {
			if (row == null) {
				continue;
			}
			long uid = longFrom(row.get("user_id"));
			if (uid > 0L) {
				uids.add(uid);
			}
		}
		Map<Long, Map<String, Object>> memByUser =
				promoterListMemberWideMergeReadService.batchRowMapsByUserIds(companyId, uids);
		List<Map<String, Object>> out = new ArrayList<>(listRows.size());
		for (Map<String, Object> row : listRows) {
			if (row == null) {
				out.add(null);
				continue;
			}
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
			long uid = longFrom(row.get("user_id"));
			Map<String, Object> mem = memByUser.get(uid);
			if (mem != null) {
				merged.putAll(mem);
			}
			merged.putAll(row);
			PromoterAdminListRowJdbcKeyCoalesce.coalesceJdbcPromoterKeysToSnakeCase(merged);
			String serializedUpdated =
					PromoterAdminListPromoterRowCompleter.applyListRowUpdatedFromPromoterJdbcRow(merged, row);
			promoterListRowUpdatedSerializedByUserId.put(uid, serializedUpdated);
			PromoterAdminListPromoterRowCompleter.fillAbsentStablePromoterColumns(merged);
			Object idVal = merged.get("id");
			if (idVal != null && !merged.containsKey("promoter_id")) {
				merged.put("promoter_id", idVal);
			}
			merged.remove("name");
			merged.remove("avatar");
			merged.remove("habbit");
			out.add(merged);
		}
		return out;
	}

	/**
	 * HTTP filter snapshot for OBJECT_WRAPPER: excludes raw mobile/username/identity_name; user_id
	 * and identity_id reflect the same predicates as the list query (caller filter normalized before
	 * {@link PromoterListQueryService#getPromoterList}).
	 */
	private static LinkedHashMap<String, Object> buildFilterSnapshotMap(
			long companyId,
			LinkedHashMap<String, Object> requestFilter,
			String storeStatusRaw,
			String timeStartBegin,
			String timeStartEnd,
			boolean hadIdentityNameQuery) {
		LinkedHashMap<String, Object> filterSnapshotMap = new LinkedHashMap<>();
		filterSnapshotMap.put("company_id", companyId);
		filterSnapshotMap.put("is_promoter", 1);
		Object snapUserId = requestFilter.get("user_id");
		if (snapUserId != null) {
			filterSnapshotMap.put("user_id", snapUserId);
		}
		if (storeStatusRaw != null && !storeStatusRaw.trim().isEmpty()) {
			filterSnapshotMap.put("shop_status", storeStatusRaw.trim());
		}
		if (timeStartBegin != null && !timeStartBegin.trim().isEmpty()) {
			filterSnapshotMap.put("created|>=", timeStartBegin.trim());
			filterSnapshotMap.put("created|<=", timeStartEnd == null ? "" : timeStartEnd.trim());
		}
		if (hadIdentityNameQuery) {
			Object ido = requestFilter.get("identity_id");
			long snapIdentity = ido == null ? -1L : longFrom(ido);
			filterSnapshotMap.put("identity_id", snapIdentity);
		}
		return filterSnapshotMap;
	}

	/**
	 * Mutates {@code filter} in place so keys match what {@link PromoterListQueryService#getPromoterList}
	 * applies internally before SQL (mobile/username resolved to {@code user_id}, identity name to
	 * {@code identity_id}, raw lookup keys removed). Safe to call once after {@code company_id} is set.
	 */
	private void applyPromoterListCallerFilterSameAsListQueryService(
			long companyId, LinkedHashMap<String, Object> filter) {
		List<Long> userIds = extractInitialUserIdList(filter.remove("user_id"));

		String mobile = readTrimmedString(filter.get("mobile"));
		if (StringUtils.hasText(mobile)) {
			List<Long> ids = memberAccountService.listUserIdsByCompanyAndMobile(companyId, mobile.trim());
			if (ids.isEmpty()) {
				userIds = List.of(-1L);
			} else {
				if (userIds != null && !userIds.isEmpty()) {
					ArrayList<Long> inter = new ArrayList<>(userIds);
					inter.retainAll(ids);
					userIds = inter.isEmpty() ? List.of(-1L) : inter;
				} else {
					userIds = new ArrayList<>(ids);
				}
			}
			filter.remove("mobile");
		}

		if (!isNoMatchUserIdList(userIds)) {
			String username = readTrimmedString(filter.get("username"));
			if (StringUtils.hasText(username)) {
				List<Long> ids = memberAccountService.listUserIdsByUsername(companyId, username.trim());
				if (ids.isEmpty()) {
					userIds = List.of(-1L);
				} else {
					if (userIds != null && !userIds.isEmpty()) {
						ArrayList<Long> inter = new ArrayList<>(userIds);
						inter.retainAll(ids);
						userIds = inter.isEmpty() ? List.of(-1L) : inter;
					} else {
						userIds = new ArrayList<>(ids);
					}
				}
				filter.remove("username");
			}
		}

		if (filter.containsKey("username")) {
			Object un = filter.get("username");
			if (un == null || !StringUtils.hasText(String.valueOf(un).trim())) {
				filter.remove("username");
			}
		}

		if (isNoMatchUserIdList(userIds)) {
			filter.put("user_id", List.of(-1L));
		} else if (userIds == null || userIds.isEmpty()) {
			filter.remove("user_id");
		} else {
			filter.put("user_id", userIds);
		}

		resolveIdentityNameToIdForFilter(companyId, filter);
	}

	private void resolveIdentityNameToIdForFilter(long companyId, LinkedHashMap<String, Object> filter) {
		Object rawName = filter.get("identity_name");
		if (rawName == null) {
			return;
		}
		String trimmed = String.valueOf(rawName).trim();
		if (!StringUtils.hasText(trimmed)) {
			filter.remove("identity_name");
			return;
		}
		PromoterIdentity one =
				promoterIdentityMapper.selectOne(
						new LambdaQueryWrapper<PromoterIdentity>()
								.eq(PromoterIdentity::getCompanyId, companyId)
								.eq(PromoterIdentity::getName, trimmed)
								.orderByDesc(PromoterIdentity::getId)
								.last("LIMIT 1"));
		long identityId = (one == null || one.getId() == null) ? -1L : one.getId();
		filter.put("identity_id", identityId);
		filter.remove("identity_name");
	}

	private static boolean isNoMatchUserIdList(List<Long> userIds) {
		return userIds != null && userIds.size() == 1 && userIds.get(0).equals(-1L);
	}

	private static List<Long> extractInitialUserIdList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list) {
			ArrayList<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				out.add(longFrom(o));
			}
			return out.isEmpty() ? null : out;
		}
		return List.of(longFrom(raw));
	}

	private static String readTrimmedString(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private static Map<String, Object> defaultBrokerageStatsNine() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("itemTotalPrice", 0L);
		m.put("rebateTotal", 0L);
		m.put("noCloseRebate", 0L);
		m.put("cashWithdrawalRebate", 0L);
		m.put("freezeCashWithdrawalRebate", 0L);
		m.put("rechargeRebate", 0L);
		m.put("payedRebate", 0L);
		m.put("rechargePoint", 0L);
		m.put("cashWithdrawalPoint", 0L);
		m.put("noClosePoint", 0L);
		m.put("pointTotal", 0L);
		return m;
	}

	private static String normalizePathSourceForSubstringSearch(String pathSourceParam) {
		if (pathSourceParam == null) {
			return "";
		}
		return pathSourceParam.trim();
	}

	private static boolean isNonEmptyShopIdValue(Object shopIdRaw) {
		if (shopIdRaw == null) {
			return false;
		}
		if (shopIdRaw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				return false;
			}
			for (Object e : col) {
				if (e == null) {
					continue;
				}
				if (e instanceof Number n) {
					if (n.longValue() != 0L) {
						return true;
					}
					continue;
				}
				if (e instanceof String s) {
					String ts = s.trim();
					if (!ts.isEmpty() && !"0".equals(ts)) {
						return true;
					}
					continue;
				}
				String ts = String.valueOf(e).trim();
				if (!ts.isEmpty() && !"0".equals(ts)) {
					return true;
				}
			}
			return false;
		}
		if (shopIdRaw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (shopIdRaw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		String t = String.valueOf(shopIdRaw).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
	}

	private static boolean distributorIdQueryTruthyForBrokerage(String distributorIdRaw) {
		if (distributorIdRaw == null) {
			return false;
		}
		String t = distributorIdRaw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return LeadingNumberParser.parseAsLong(t) > 0L;
	}

	/**
	 * Whether {@code shop_id} on the gate filter is exactly one positive id matching the leading
	 * numeric {@code distributor_id} with {@code is_all=false}, and the operator is not on the
	 * merchant-wide shop scope. Used when the salesperson gate yields no user ids: keep listing by
	 * shop only for this narrow path, otherwise force no rows via {@code user_id = -1}.
	 */
	private static boolean isDistributorIdSingleShopNarrowOnly(
			String operatorType,
			boolean isAll,
			long distributorIdLeading,
			Object shopIdRaw) {
		if ("merchant".equalsIgnoreCase(operatorType) || isAll || distributorIdLeading <= 0L) {
			return false;
		}
		List<Long> shops = normalizeShopIdListForGate(shopIdRaw);
		return shops.size() == 1 && shops.get(0) == distributorIdLeading;
	}

	private static List<Long> normalizeShopIdListForGate(Object shopRaw) {
		if (!(shopRaw instanceof List<?> list)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			long v = longFrom(o);
			if (v > 0L) {
				out.add(v);
			}
		}
		return out.isEmpty() ? List.of() : out;
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
