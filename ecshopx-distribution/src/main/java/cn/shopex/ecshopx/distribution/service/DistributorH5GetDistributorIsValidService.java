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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.integration.MapGeocodeClient;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidAppendFieldsService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidCoreQueryService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidNearShopService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidNostoresBranchService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidSettingService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidWhiteListBranchService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.dto.DistributorIsValidQuery;
import cn.shopex.ecshopx.members.service.address.MemberAddressDefaultReadService;
import cn.shopex.ecshopx.members.service.address.MemberAddressLatLngNonNumericEnqueueService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorH5GetDistributorIsValidService {

	private final DistributorIsValidSettingService distributorIsValidSettingService;
	private final DistributorIsValidWhiteListBranchService distributorIsValidWhiteListBranchService;
	private final MemberAddressDefaultReadService memberAddressDefaultReadService;
	private final MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService;
	private final MapGeocodeClient mapGeocodeClient;
	private final DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;
	private final DistributorIsValidNostoresBranchService distributorIsValidNostoresBranchService;
	private final DistributorIsValidNearShopService distributorIsValidNearShopService;
	private final DistributorIsValidAppendFieldsService distributorIsValidAppendFieldsService;
	private final DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService;

	public DistributorH5GetDistributorIsValidService(
			DistributorIsValidSettingService distributorIsValidSettingService,
			DistributorIsValidWhiteListBranchService distributorIsValidWhiteListBranchService,
			MemberAddressDefaultReadService memberAddressDefaultReadService,
			MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService,
			MapGeocodeClient mapGeocodeClient,
			DistributorIsValidCoreQueryService distributorIsValidCoreQueryService,
			DistributorIsValidNostoresBranchService distributorIsValidNostoresBranchService,
			DistributorIsValidNearShopService distributorIsValidNearShopService,
			DistributorIsValidAppendFieldsService distributorIsValidAppendFieldsService,
			DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService) {
		this.distributorIsValidSettingService = distributorIsValidSettingService;
		this.distributorIsValidWhiteListBranchService = distributorIsValidWhiteListBranchService;
		this.memberAddressDefaultReadService = memberAddressDefaultReadService;
		this.memberAddressLatLngNonNumericEnqueueService = memberAddressLatLngNonNumericEnqueueService;
		this.mapGeocodeClient = mapGeocodeClient;
		this.distributorIsValidCoreQueryService = distributorIsValidCoreQueryService;
		this.distributorIsValidNostoresBranchService = distributorIsValidNostoresBranchService;
		this.distributorIsValidNearShopService = distributorIsValidNearShopService;
		this.distributorIsValidAppendFieldsService = distributorIsValidAppendFieldsService;
		this.distributionStoreEntryRuleRedisService = distributionStoreEntryRuleRedisService;
	}

	public Map<String, Object> getDistributorIsValid(
			long companyId, long userId, long filterCompanyId, long authCompanyId, DistributorIsValidQuery query) {
		Map<String, Object> result = new LinkedHashMap<>();
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", Long.valueOf(companyId));
		filter.put("is_valid", "true");
		Map<String, Object> setting = distributorIsValidSettingService.getOpenDividedSetting(companyId);
		if (Boolean.TRUE.equals(setting.get("status"))
				&& query.showType() != null
				&& "self".equals(query.showType())) {
			return distributorIsValidWhiteListBranchService.getWhiteListDistributor(
					userId, companyId, query.distributorIdRaw(), query.lngRaw(), query.latRaw());
		}
		if (Boolean.TRUE.equals(setting.get("status"))) {
			filter.put("open_divided", Integer.valueOf(0));
		}
		int radioType = resolveRadioType(companyId);
		boolean preferVirtualStore = query.preferVirtualStoreByInRule(radioType);
		boolean skipDefaultDistributorFallback = false;
		boolean triedDefaultDistributor = false;
		String[] work;
		if (preferVirtualStore) {
			work = new String[] {query.lngRaw(), query.latRaw()};
		} else {
			work =
					fillLngLatFromMemberAddressIfNeeded(
							userId, filterCompanyId, authCompanyId, query.lngRaw(), query.latRaw());
		}
		String lngWork = work[0];
		String latWork = work[1];
		validateLngLatSometimes(lngWork, latWork);
		if (distributorIdRawLooksSet(query.distributorIdRaw())) {
			filter.put("distributor_id", resolveDistributorIdForFilter(query.distributorIdRaw()));
			filter.remove("open_divided");
			result = new LinkedHashMap<>(distributorIsValidCoreQueryService.getInfo(filter));
			if (result.isEmpty()) {
				throw new ResourceException("店铺查询错误.");
			}
			result.put("status", Boolean.TRUE);
			if ("true".equals(String.valueOf(result.get("is_valid")))) {
				result.put("status", Boolean.FALSE);
			}
			result.put("old_valid", Boolean.TRUE);
			applyBranchAPhoneAliases(result);
		} else if (query.isNostores() == 1) {
			result =
					distributorIsValidNostoresBranchService.runNostoresBranch(
							companyId, userId, filter, lngWork, latWork, query);
		} else if (coordinatesLookPresent(lngWork, latWork)) {
			filter.remove("distributor_id");
			int isShopDivided = Boolean.TRUE.equals(setting.get("status")) ? 1 : 0;
			if (isShopDivided == 1) {
				filter.put("distributor_self", Integer.valueOf(0));
			}
			double latNum = Double.parseDouble(latWork.trim());
			double lngNum = Double.parseDouble(lngWork.trim());
			result =
					distributorIsValidNearShopService.getNearShopData(filter, latNum, lngNum, isShopDivided);
			if (isShopDivided == 0 && distributorIdExplicitlyZero(query.distributorIdRaw())) {
				Object ds = result.get("distributor_self");
				if (numericOneTruthy(ds)) {
					result.put("distributor_id", Long.valueOf(0L));
				}
			}
		} else {
			if (distributorIdExplicitlyZero(query.distributorIdRaw())) {
				if (userId == 0L) {
					if (Boolean.TRUE.equals(setting.get("status"))) {
						Map<String, Object> f = new LinkedHashMap<>();
						f.put("company_id", companyId);
						f.put("distributor_id", Long.valueOf(0L));
						result = distributorIsValidCoreQueryService.getInfo(f);
					} else {
						result = distributorIsValidCoreQueryService.getDistributorSelf(companyId, true, Map.of());
						result.put("distributor_id", Long.valueOf(0L));
					}
				} else {
					if (Boolean.TRUE.equals(setting.get("status"))) {
						result =
								distributorIsValidCoreQueryService.getDistributorSelf(
										companyId, true, Map.of("open_divided", Integer.valueOf(0)));
					} else {
						result = distributorIsValidCoreQueryService.getDistributorSelf(companyId, true, Map.of());
						result.put("distributor_id", Long.valueOf(0L));
					}
				}
				result.put("is_delivery", result.getOrDefault("is_delivery", Boolean.TRUE));
				result.put("is_ziti", result.getOrDefault("is_ziti", Boolean.FALSE));
				result.put("is_valid", "true");
			} else if (preferVirtualStore) {
				skipDefaultDistributorFallback = true;
				result = loadPreferVirtualStoreResult(companyId);
			} else {
				triedDefaultDistributor = true;
				result =
						distributorIsValidCoreQueryService.getInfoSimple(companyId, 1, "true");
				if (result == null) {
					result = new LinkedHashMap<>();
				}
			}
		}
		if (applyFirstEmptyIsValidTerminal(
				companyId, result, skipDefaultDistributorFallback, triedDefaultDistributor)) {
			return result;
		}
		if (isUnsetIsValidFlag(result.get("is_valid"))) {
			result.clear();
			result.put("distributor_id", Long.valueOf(0L));
			result.put("is_delivery", Boolean.TRUE);
			result.put("is_ziti", Boolean.FALSE);
			return result;
		}
		if ("true".equals(String.valueOf(result.get("is_valid")))) {
			distributorIsValidAppendFieldsService.applyTailFields(companyId, result, query);
			overwriteDividedHeadStoreMinimalIfNeeded(setting, query.distributorIdRaw(), result);
			return result;
		}
		Map<String, Object> fallback = distributorIsValidCoreQueryService.getInfoSimple(companyId, 1, "true");
		if (fallback == null || fallback.isEmpty()) {
			result.clear();
			result.put("distributor_id", Long.valueOf(0L));
			result.put("is_delivery", Boolean.TRUE);
			result.put("is_ziti", Boolean.FALSE);
		} else {
			result = new LinkedHashMap<>(fallback);
		}
		return result;
	}

	private void applyBranchAPhoneAliases(Map<String, Object> result) {
		result.put("phone", result.get("mobile"));
		result.put("store_address", result.get("address"));
		result.put("store_name", result.get("name"));
	}

	private String[] fillLngLatFromMemberAddressIfNeeded(
			long userId, long filterCompanyId, long authCompanyId, String lngWork, String latWork) {
		String lng = lngWork;
		String lat = latWork;
		if (userId <= 0L || coordinatesLookPresent(lng, lat)) {
			return new String[] {lng, lat};
		}
		Map<String, Object> addr = memberAddressDefaultReadService.getDefaultAddress(filterCompanyId, authCompanyId);
		if (addr != null) {
			memberAddressLatLngNonNumericEnqueueService.enqueueIfNeeded(filterCompanyId, authCompanyId, addr);
			Object la = addr.get("lat");
			Object ln = addr.get("lng");
			if (la != null) {
				lat = String.valueOf(la);
			}
			if (ln != null) {
				lng = String.valueOf(ln);
			}
			if ((!StringUtils.hasText(lat) || !StringUtils.hasText(lng))
					&& StringUtils.hasText(stringVal(addr.get("city")))
					&& StringUtils.hasText(stringVal(addr.get("adrdetail")))) {
				MapGeocodeClient.LngLat geo =
						mapGeocodeClient.getLatAndLng(
								filterCompanyId, stringVal(addr.get("city")), stringVal(addr.get("adrdetail")));
				lng = geo.lng();
				lat = geo.lat();
			}
		}
		return new String[] {lng, lat};
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private boolean coordinatesLookPresent(String lng, String lat) {
		if (lng == null || lat == null) {
			return false;
		}
		String lt = lng.trim();
		String la = lat.trim();
		if (lt.isEmpty() || la.isEmpty()) {
			return false;
		}
		double ln;
		double laNum;
		try {
			ln = Double.parseDouble(lt);
			laNum = Double.parseDouble(la);
		} catch (NumberFormatException e) {
			return false;
		}
		if (!Double.isFinite(ln) || !Double.isFinite(laNum)) {
			return false;
		}
		return ln != 0d && laNum != 0d;
	}

	private boolean distributorIdRawLooksSet(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("false".equalsIgnoreCase(t)) {
			return true;
		}
		return true;
	}

	private boolean isUnsetIsValidFlag(Object v) {
		if (v == null) {
			return true;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return true;
		}
		if ("0".equals(s)) {
			return true;
		}
		return "false".equalsIgnoreCase(s);
	}

	private boolean distributorIdExplicitlyZero(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return parseQueryDistributorIdAsLong(raw) == 0L;
	}

	private long parseQueryDistributorIdAsLong(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty() || "false".equalsIgnoreCase(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private Long resolveDistributorIdForFilter(String raw) {
		if (raw != null && "false".equalsIgnoreCase(raw.trim())) {
			return Long.valueOf(0L);
		}
		return Long.valueOf(parseQueryDistributorIdAsLong(raw));
	}

	private void validateLngLatSometimes(String lngWork, String latWork) {
		double[] lngBuf = new double[1];
		double[] latBuf = new double[1];
		boolean okLng = tryParseFiniteDouble(lngWork, lngBuf);
		boolean okLat = tryParseFiniteDouble(latWork, latBuf);
		if (!okLng || !okLat) {
			return;
		}
		double lng = lngBuf[0];
		double lat = latBuf[0];
		if (lng < -180.0d || lng > 180.0d || lat < -90.0d || lat > 90.0d) {
			Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
			fieldErrors.put("lng", List.of("经度超出允许范围"));
			fieldErrors.put("lat", List.of("纬度超出允许范围"));
			throw new ResourceException("经纬度范围错误.", fieldErrors);
		}
	}

	private static boolean tryParseFiniteDouble(String raw, double[] out) {
		if (raw == null || raw.trim().isEmpty()) {
			return false;
		}
		try {
			double v = Double.parseDouble(raw.trim());
			if (!Double.isFinite(v)) {
				return false;
			}
			out[0] = v;
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private Map<String, Object> loadPreferVirtualStoreResult(long companyId) {
		Map<String, Object> loaded =
				new LinkedHashMap<>(
						distributorIsValidCoreQueryService.getDistributorSelf(companyId, true, Map.of()));
		if (!loaded.isEmpty()) {
			loaded.put("is_delivery", loaded.getOrDefault("is_delivery", Boolean.TRUE));
			loaded.put("is_ziti", loaded.getOrDefault("is_ziti", Boolean.FALSE));
			loaded.put("is_valid", "true");
		}
		return loaded;
	}

	private int resolveRadioType(long companyId) {
		Map<String, Object> inRule = distributionStoreEntryRuleRedisService.getInRule(companyId);
		Object raw = inRule.get("radio_type");
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw != null) {
			try {
				return Integer.parseInt(String.valueOf(raw).trim());
			} catch (NumberFormatException e) {
				return 1;
			}
		}
		return 1;
	}

	private boolean applyFirstEmptyIsValidTerminal(
			long companyId,
			Map<String, Object> result,
			boolean skipDefaultDistributorFallback,
			boolean triedDefaultDistributor) {
		if (!isUnsetIsValidFlag(result.get("is_valid"))) {
			return false;
		}
		if (!skipDefaultDistributorFallback && !triedDefaultDistributor) {
			Map<String, Object> fallback =
					distributorIsValidCoreQueryService.getInfoSimple(companyId, 1, "true");
			if (fallback == null || isUnsetIsValidFlag(fallback.get("is_valid"))) {
				result.clear();
				result.put("distributor_id", Long.valueOf(0L));
				result.put("is_delivery", Boolean.TRUE);
				result.put("is_ziti", Boolean.FALSE);
			} else {
				result.clear();
				result.putAll(fallback);
			}
		} else if (result.isEmpty()) {
			result.put("distributor_id", Long.valueOf(0L));
			result.put("is_delivery", Boolean.TRUE);
			result.put("is_ziti", Boolean.FALSE);
		}
		return true;
	}

	private boolean dividedHeadDefaultLooksTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return "1".equals(t) || "true".equalsIgnoreCase(t);
		}
		return false;
	}

	private void overwriteDividedHeadStoreMinimalIfNeeded(
			Map<String, Object> setting, String postdataDistributorIdRaw, Map<String, Object> mutableResult) {
		if (!Boolean.TRUE.equals(setting.get("status"))) {
			return;
		}
		if (postdataDistributorIdRaw == null) {
			return;
		}
		if (parseQueryDistributorIdAsLong(postdataDistributorIdRaw) != 0L) {
			return;
		}
		if (!dividedHeadDefaultLooksTrue(mutableResult.get("is_default"))) {
			return;
		}
		mutableResult.clear();
		mutableResult.put("distributor_id", Long.valueOf(0L));
		mutableResult.put("is_delivery", Boolean.TRUE);
		mutableResult.put("is_ziti", Boolean.FALSE);
	}

	private static boolean numericOneTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(String.valueOf(v).trim());
	}
}
