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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorAftersalesAddress;
import cn.shopex.ecshopx.distribution.mapper.DistributorAftersalesAddressMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.multilang.DistributorAftersalesAddressOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorAftersalesAddressReadService {

	private final DistributorAftersalesAddressMapper distributorAftersalesAddressMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;
	private final DistributorAftersalesAddressOutsideMultiLangReadService aftersalesAddressOutsideMultiLangReadService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final DistributorMapper distributorMapper;
	private final ObjectMapper objectMapper;

	public DistributorAftersalesAddressReadService(
			DistributorAftersalesAddressMapper distributorAftersalesAddressMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorBatchApiRowQueryService distributorBatchApiRowQueryService,
			DistributorAftersalesAddressOutsideMultiLangReadService aftersalesAddressOutsideMultiLangReadService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			DistributorMapper distributorMapper,
			ObjectMapper objectMapper) {
		this.distributorAftersalesAddressMapper = distributorAftersalesAddressMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorBatchApiRowQueryService = distributorBatchApiRowQueryService;
		this.aftersalesAddressOutsideMultiLangReadService = aftersalesAddressOutsideMultiLangReadService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.distributorMapper = distributorMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getNearAftersalesLocation(
			long companyId,
			String distributorIdRaw,
			String distributorNameRaw,
			String lngRaw,
			String latRaw,
			String requestLangTag) {
		String lngTrim = blankToNull(lngRaw);
		String latTrim = blankToNull(latRaw);
		BigDecimal lngBd = null;
		BigDecimal latBd = null;
		if (lngTrim != null && latTrim != null) {
			lngBd = parseLongitudeOrThrow(lngTrim);
			latBd = parseLatitudeOrThrow(latTrim);
		} else if (lngTrim != null) {
			lngBd = parseLongitudeOrThrow(lngTrim);
		} else if (latTrim != null) {
			latBd = parseLatitudeOrThrow(latTrim);
		}
		boolean useDistance = lngBd != null
				&& latBd != null
				&& coordinateDecimalLooksPresent(lngBd)
				&& coordinateDecimalLooksPresent(latBd);

		Long distributorFilter = null;
		if (StringUtils.hasText(distributorIdRaw == null ? "" : distributorIdRaw.trim())) {
			try {
				long parsed = Long.parseLong(distributorIdRaw.trim());
				if (parsed > 0L) {
					distributorFilter = parsed;
				}
			} catch (NumberFormatException ignored) {
				// treat as self branch
			}
		}

		String trimmedName = distributorNameRaw == null ? "" : distributorNameRaw.trim();

		Optional<Map<String, Object>> headOpt =
				distributorRepositoryGetInfoSimpleService.findH5OfflineAftersalesHeadRow(companyId, distributorFilter);
		if (headOpt.isEmpty()) {
			return emptyNearAftersalesPayload();
		}

		Map<String, Object> distributorInfo = new LinkedHashMap<>(headOpt.get());
		distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, List.of(distributorInfo));

		List<Long> addressDistributorIds = new ArrayList<>();
		List<Long> filteredDistributorIdsForName = new ArrayList<>();

		Object offlineLinkedRaw = distributorInfo.get("offline_aftersales_distributor_id");
		if (isTruthyOfflineAftersalesDistributorId(offlineLinkedRaw)) {
			String asText = offlineLinkedRaw instanceof String s ? s : offlineLinkedRaw.toString();
			List<Long> linkedIds = parseOfflineAftersalesDistributorIdList(asText, objectMapper);
			if (!linkedIds.isEmpty()) {
				List<Distributor> otherEntities = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
						.eq(Distributor::getCompanyId, companyId)
						.in(Distributor::getDistributorId, linkedIds)
						.eq(Distributor::getIsValid, "true")
						.eq(Distributor::getOfflineAftersalesOther, 1)
						.select(Distributor::getDistributorId, Distributor::getName));
				List<Map<String, Object>> otherRows = new ArrayList<>(otherEntities.size());
				for (Distributor d : otherEntities) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					row.put("distributor_id", d.getDistributorId());
					row.put("name", d.getName());
					otherRows.add(row);
				}
				distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, otherRows);
				for (Map<String, Object> row : otherRows) {
					Object didObj = row.get("distributor_id");
					if (didObj instanceof Number n) {
						addressDistributorIds.add(n.longValue());
					}
				}
				if (!trimmedName.isEmpty()) {
					for (Map<String, Object> row : otherRows) {
						Object nameObj = row.get("name");
						if (nameObj != null && nameObj.toString().contains(trimmedName)) {
							filteredDistributorIdsForName.add(toLongDistributorId(row.get("distributor_id")));
						}
					}
				}
			}
		}

		if ("true".equals(String.valueOf(distributorInfo.get("is_valid")).trim())) {
			Object v = distributorInfo.get("offline_aftersales_self");
			boolean selfOn = (v instanceof Number n && n.intValue() != 0)
					|| Boolean.TRUE.equals(v)
					|| "1".equals(String.valueOf(v).trim())
					|| (v instanceof String s
							&& !s.isBlank()
							&& !"0".equals(s.trim())
							&& !"false".equalsIgnoreCase(s.trim()));
			if (selfOn) {
				addressDistributorIds.add(toLongDistributorId(distributorInfo.get("distributor_id")));
			}
		}

		if (addressDistributorIds.isEmpty()) {
			return emptyNearAftersalesPayload();
		}

		List<Long> sqlIds = new ArrayList<>(new LinkedHashSet<>(addressDistributorIds));
		long sqlTotalCount = distributorAftersalesAddressMapper.countOfflineNear(companyId, sqlIds);
		if (sqlTotalCount == 0L) {
			return emptyNearAftersalesPayload();
		}

		List<Map<String, Object>> addressRows =
				distributorAftersalesAddressMapper.selectOfflineNear(companyId, sqlIds, lngBd, latBd, useDistance);

		for (Map<String, Object> r : addressRows) {
			Object mob = r.get("mobile");
			if (mob != null && StringUtils.hasText(mob.toString())) {
				r.put("mobile", sensitiveFieldEncryptor.decrypt(mob.toString()));
			}
			Object ct = r.get("contact");
			if (ct != null && StringUtils.hasText(ct.toString())) {
				r.put("contact", sensitiveFieldEncryptor.decrypt(ct.toString()));
			}
		}

		aftersalesAddressOutsideMultiLangReadService.overlayPrimaryFieldsForList(companyId, requestLangTag, addressRows);
		aftersalesAddressOutsideMultiLangReadService.applyLangMaps(companyId, requestLangTag, addressRows);

		List<Map<String, Object>> listForResponse;
		if (trimmedName.isEmpty()) {
			listForResponse = new ArrayList<>(addressRows);
		} else {
			listForResponse = addressRows.stream()
					.filter(r -> filteredDistributorIdsForName.contains(distributorIdOfRow(r))
							|| (r.get("address") != null
									&& r.get("address").toString().contains(trimmedName)))
					.collect(Collectors.toCollection(ArrayList::new));
		}

		long totalCountForResponse = trimmedName.isEmpty() ? sqlTotalCount : (long) listForResponse.size();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCountForResponse);
		out.put("list", listForResponse);
		return out;
	}

	private Map<String, Object> emptyNearAftersalesPayload() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 0L);
		out.put("list", new ArrayList<>());
		return out;
	}

	private static long toLongDistributorId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long distributorIdOfRow(Map<String, Object> r) {
		return toLongDistributorId(r.get("distributor_id"));
	}

	private static boolean isTruthyOfflineAftersalesDistributorId(Object fromMap) {
		if (fromMap == null) {
			return false;
		}
		if (fromMap instanceof List<?> list) {
			return !list.isEmpty();
		}
		String s = fromMap.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"[]".equals(s);
	}

	private static List<Long> parseOfflineAftersalesDistributorIdList(String jsonOrCsv, ObjectMapper om) {
		if (!StringUtils.hasText(jsonOrCsv)) {
			return List.of();
		}
		String t = jsonOrCsv.trim();
		try {
			JsonNode root = om.readTree(t);
			if (root != null && root.isArray()) {
				List<Long> out = new ArrayList<>();
				for (JsonNode n : root) {
					if (n == null || n.isNull()) {
						continue;
					}
					if (n.isNumber()) {
						long v = n.longValue();
						if (v > 0L) {
							out.add(v);
						}
					} else if (n.isTextual()) {
						String ts = n.asText().trim();
						if (StringUtils.hasText(ts)) {
							try {
								long v = Long.parseLong(ts);
								if (v > 0L) {
									out.add(v);
								}
							} catch (NumberFormatException ignored) {
								// skip
							}
						}
					}
				}
				return out;
			}
		} catch (Exception ignored) {
			// fall through to CSV
		}
		List<Long> out = new ArrayList<>();
		for (String part : t.split(",")) {
			String p = part.trim();
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				long v = Long.parseLong(p);
				if (v > 0L) {
					out.add(v);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private static boolean coordinateDecimalLooksPresent(BigDecimal v) {
		if (v == null) {
			return false;
		}
		String plain = v.stripTrailingZeros().toPlainString();
		return !plain.isEmpty() && !"0".equals(plain);
	}

	private static String blankToNull(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw.trim();
	}

	private static BigDecimal parseLongitudeOrThrow(String trimmed) {
		try {
			BigDecimal v = new BigDecimal(trimmed);
			if (v.compareTo(new BigDecimal("-180")) < 0 || v.compareTo(new BigDecimal("180")) > 0) {
				throw new BadRequestException("请授权当前所在位置或传入正确的经纬度");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("请授权当前所在位置或传入正确的经纬度");
		}
	}

	private static BigDecimal parseLatitudeOrThrow(String trimmed) {
		try {
			BigDecimal v = new BigDecimal(trimmed);
			if (v.compareTo(new BigDecimal("-90")) < 0 || v.compareTo(new BigDecimal("90")) > 0) {
				throw new BadRequestException("请授权当前所在位置或传入正确的经纬度");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("请授权当前所在位置或传入正确的经纬度");
		}
	}

	public Map<String, Object> getAftersaleAddressByDistributor(
			long companyId, long distributorId, String requestLangTag) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getReturnType, "logistics");
		long total = distributorAftersalesAddressMapper.selectCount(w);
		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0) {
			w.orderByDesc(DistributorAftersalesAddress::getAddressId);
			List<DistributorAftersalesAddress> rows = distributorAftersalesAddressMapper.selectList(w);
			for (DistributorAftersalesAddress row : rows) {
				list.add(toAddressApiRow(row));
			}
			aftersalesAddressOutsideMultiLangReadService.overlayPrimaryFieldsForList(companyId, requestLangTag, list);
			aftersalesAddressOutsideMultiLangReadService.applyLangMaps(companyId, requestLangTag, list);
		}
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("total_count", total);
		address.put("list", list);
		Optional<Map<String, Object>> opt =
				distributorRepositoryGetInfoSimpleService.getDistributorApiRowByCompanyAndDistributorId(
						companyId, distributorId);
		Object distributorInfo;
		if (opt.isEmpty()) {
			distributorInfo = Collections.emptyList();
		} else {
			Map<String, Object> d = new LinkedHashMap<>(opt.get());
			distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, List.of(d));
			distributorInfo = d;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("address", address);
		out.put("distributor_info", distributorInfo);
		return out;
	}

	/** Whether an offline return address exists for the company and distributor. */
	public boolean existsOfflineAftersalesAddress(long companyId, long distributorId) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getReturnType, "offline")
				.last("LIMIT 1");
		return distributorAftersalesAddressMapper.selectCount(w) > 0;
	}

	public Map<String, Object> readOfflineAftersalesAddress(long distributorId) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
				.eq(DistributorAftersalesAddress::getReturnType, "offline")
				.last("LIMIT 1");
		DistributorAftersalesAddress row = distributorAftersalesAddressMapper.selectOne(w);
		if (row == null) {
			return null;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		put(m, "address_id", row.getAddressId());
		put(m, "distributor_id", row.getDistributorId());
		put(m, "company_id", row.getCompanyId());
		put(m, "province", row.getProvince());
		put(m, "city", row.getCity());
		put(m, "area", row.getArea());
		put(m, "regions_id", regionsJsonColumnForApi(row.getRegionsId()));
		put(m, "regions", regionsJsonColumnForApi(row.getRegions()));
		put(m, "address", row.getAddress());
		put(m, "lng", row.getLng());
		put(m, "lat", row.getLat());
		put(m, "contact", row.getContact());
		put(m, "mobile", row.getMobile());
		put(m, "post_code", row.getPostCode());
		put(m, "is_default", row.getIsDefault());
		put(m, "created", row.getCreated());
		put(m, "updated", row.getUpdated());
		put(m, "merchant_id", row.getMerchantId());
		put(m, "name", row.getName());
		put(m, "hours", row.getHours());
		put(m, "return_type", row.getReturnType());
		put(m, "supplier_id", row.getSupplierId());
		return m;
	}

	public Map<String, Object> getDistributorAfterSalesAddress(
			long companyId,
			long merchantIdJwt,
			String operatorType,
			long operatorId,
			int page,
			int pageSize,
			String province,
			String city,
			String area,
			String distributorIdRaw,
			String requestLangTag) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getReturnType, "logistics");
		if ("merchant".equalsIgnoreCase(operatorType)) {
			w.eq(DistributorAftersalesAddress::getMerchantId, merchantIdJwt);
		} else if ("supplier".equalsIgnoreCase(operatorType)) {
			w.eq(DistributorAftersalesAddress::getSupplierId, (int) operatorId);
		} else {
			w.eq(DistributorAftersalesAddress::getSupplierId, 0);
		}
		if (StringUtils.hasText(province)) {
			w.like(DistributorAftersalesAddress::getProvince, province.trim());
		}
		if (StringUtils.hasText(city)) {
			w.like(DistributorAftersalesAddress::getCity, city.trim());
		}
		if (StringUtils.hasText(area)) {
			w.like(DistributorAftersalesAddress::getArea, area.trim());
		}
		String distTrim = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		if (StringUtils.hasText(distTrim)) {
			try {
				long did = Long.parseLong(distTrim);
				w.eq(DistributorAftersalesAddress::getDistributorId, did);
			} catch (NumberFormatException ignored) {
				// Loose compatibility: omit filter when not numeric.
			}
		}
		w.orderByDesc(DistributorAftersalesAddress::getDistributorId);

		Page<DistributorAftersalesAddress> p = new Page<>(page, pageSize);
		distributorAftersalesAddressMapper.selectPage(p, w);
		List<DistributorAftersalesAddress> records = p.getRecords();
		if (records.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", p.getTotal());
			empty.put("list", List.of());
			return empty;
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributorAftersalesAddress row : records) {
			list.add(toAddressListRow(row));
		}

		aftersalesAddressOutsideMultiLangReadService.overlayPrimaryFieldsForList(companyId, requestLangTag, list);

		Set<Long> distributorIds = new LinkedHashSet<>();
		for (Map<String, Object> addrRow : list) {
			Object didObj = addrRow.get("distributor_id");
			if (didObj instanceof Number n) {
				distributorIds.add(n.longValue());
			} else if (didObj != null && StringUtils.hasText(didObj.toString())) {
				try {
					distributorIds.add(Long.parseLong(didObj.toString().trim()));
				} catch (NumberFormatException ignored) {
					// omit
				}
			}
		}
		Map<Long, Map<String, Object>> distMaps =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(companyId, distributorIds);
		for (Map<String, Object> addrRow : list) {
			long did = ((Number) addrRow.get("distributor_id")).longValue();
			Map<String, Object> d = distMaps.get(did);
			if (d != null) {
				addrRow.put("name", d.get("name"));
				addrRow.put("logo", d.get("logo"));
			}
		}

		aftersalesAddressOutsideMultiLangReadService.applyLangMaps(companyId, requestLangTag, list);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", p.getTotal());
		out.put("list", list);
		return out;
	}

	public Map<String, Object> getDistributorAfterSalesAddressDetail(
			long companyId, long addressId, String requestLangTag) {
		LambdaQueryWrapper<DistributorAftersalesAddress> w = new LambdaQueryWrapper<>();
		w.eq(DistributorAftersalesAddress::getCompanyId, companyId)
				.eq(DistributorAftersalesAddress::getAddressId, addressId)
				.last("LIMIT 1");
		DistributorAftersalesAddress row = distributorAftersalesAddressMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("地址不存在");
		}
		Map<String, Object> addrRow = toAddressApiRow(row);
		long did = row.getDistributorId() == null ? 0L : row.getDistributorId();
		Map<Long, Map<String, Object>> distMaps =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(
						companyId, java.util.List.of(did));
		Map<String, Object> d = distMaps.get(did);
		String nameOut = (d != null && d.get("name") != null) ? d.get("name").toString() : "";
		String logoOut = (d != null && d.get("logo") != null) ? d.get("logo").toString() : "";
		addrRow.put("name", nameOut);
		addrRow.put("logo", logoOut);
		aftersalesAddressOutsideMultiLangReadService.applyLangMaps(
				companyId, requestLangTag, java.util.List.of(addrRow));
		return addrRow;
	}

	private Map<String, Object> toAddressListRow(DistributorAftersalesAddress row) {
		return toAddressApiRow(row);
	}

	private Map<String, Object> toAddressApiRow(DistributorAftersalesAddress row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("address_id", row.getAddressId());
		m.put("distributor_id", row.getDistributorId());
		m.put("company_id", row.getCompanyId());
		m.put("province", row.getProvince());
		m.put("city", row.getCity());
		m.put("area", row.getArea());
		m.put("regions_id", regionsJsonColumnForApi(row.getRegionsId()));
		m.put("regions", regionsJsonColumnForApi(row.getRegions()));
		m.put("address", row.getAddress());
		m.put("lng", row.getLng());
		m.put("lat", row.getLat());
		m.put("contact", sensitiveFieldEncryptor.decrypt(row.getContact()));
		m.put("mobile", sensitiveFieldEncryptor.decrypt(row.getMobile()));
		m.put("post_code", row.getPostCode());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("is_default", row.getIsDefault());
		m.put("merchant_id", row.getMerchantId());
		m.put("name", row.getName());
		m.put("hours", row.getHours());
		m.put("return_type", row.getReturnType());
		m.put("supplier_id", row.getSupplierId());
		return m;
	}

	/**
	 * Region id / name JSON columns are returned as stored text so JSON bodies keep the same scalar
	 * string shape as persisted values (including {@code []} stored as the two-character string).
	 */
	private static String regionsJsonColumnForApi(Object raw) {
		if (raw == null) {
			return null;
		}
		return raw.toString();
	}

	private static void put(Map<String, Object> m, String k, Object v) {
		m.put(k, v);
	}
}
