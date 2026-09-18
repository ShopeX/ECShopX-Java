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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist;

import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListFilter;
import cn.shopex.ecshopx.distribution.repository.WxappDiscountCardDistributorIdsJdbcRepository;
import cn.shopex.ecshopx.distribution.repository.WxappItemsSearchDistributorIdsJdbcRepository;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorTagRelQueryService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListFilterBuildResult;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListQuery;
import cn.shopex.ecshopx.orders.service.nostores.DistributorNostoresCartDistributorIdsReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorWxappShopListFilterBuildService {

	private final CompanyMapGeocodePort companyMapGeocodePort;
	private final WxappItemsSearchDistributorIdsJdbcRepository itemsSearchDistributorIdsJdbcRepository;
	private final WxappDiscountCardDistributorIdsJdbcRepository discountCardDistributorIdsJdbcRepository;
	private final DistributorNostoresCartDistributorIdsReadService distributorNostoresCartDistributorIdsReadService;
	private final DistributorTagRelQueryService distributorTagRelQueryService;

	public DistributorWxappShopListFilterBuildService(
			CompanyMapGeocodePort companyMapGeocodePort,
			WxappItemsSearchDistributorIdsJdbcRepository itemsSearchDistributorIdsJdbcRepository,
			WxappDiscountCardDistributorIdsJdbcRepository discountCardDistributorIdsJdbcRepository,
			DistributorNostoresCartDistributorIdsReadService distributorNostoresCartDistributorIdsReadService,
			DistributorTagRelQueryService distributorTagRelQueryService) {
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.itemsSearchDistributorIdsJdbcRepository = itemsSearchDistributorIdsJdbcRepository;
		this.discountCardDistributorIdsJdbcRepository = discountCardDistributorIdsJdbcRepository;
		this.distributorNostoresCartDistributorIdsReadService = distributorNostoresCartDistributorIdsReadService;
		this.distributorTagRelQueryService = distributorTagRelQueryService;
	}

	public DistributorWxappShopListFilterBuildResult build(
			long companyId, long userId, DistributorWxappShopListQuery q, java.util.Map<String, Object> defaultAddressRowOrNull) {
		DistributorWxappShopListFilter f = new DistributorWxappShopListFilter();
		f.setCompanyId(companyId);
		int isRecommend = 0;
		f.setProvinceLikeEscaped(null);
		f.setCityLikeEscaped(null);
		f.setAreaLikeEscaped(null);
		f.setUserLng(null);
		f.setUserLat(null);
		f.setGeoEnabled(false);

		int t = q.type();
		if (t == 1) {
			applyType1(f, q, defaultAddressRowOrNull);
		} else if (t == 2) {
			applyType2(f, defaultAddressRowOrNull, companyId);
		} else if (t == 3) {
			applyType3(f, q, companyId);
		} else {
			isRecommend = applyTypeDefault(f, q, defaultAddressRowOrNull, companyId);
		}

		f.setGeoEnabled(StringUtils.hasText(f.getUserLng()) && StringUtils.hasText(f.getUserLat()));

		if (StringUtils.hasText(q.name())) {
			String escaped = DistributorListQueryService.escapeSqlLike(q.name().trim());
			f.setNameLikeEscaped(escaped);
			f.setSearchType(Integer.valueOf(q.searchType()));
			if (q.searchType() == 1) {
				List<Long> itemHits =
						itemsSearchDistributorIdsJdbcRepository.listDistributorIdsByCompanyAndItemNameLike(companyId, escaped);
				f.setOrDistributorIdsFromItems(itemHits);
			} else {
				f.setOrDistributorIdsFromItems(null);
			}
		}

		parseDistributorIdConstraints(f, q);

		if (q.isZiti() != null) {
			f.setIsZiti(q.isZiti());
		}
		if (q.isDelivery() != null) {
			f.setIsDelivery(q.isDelivery());
		}
		if (q.isDada() != null) {
			f.setIsDada(q.isDada());
		}
		if (q.distributorCategoryIdRaw() != null && !"".equals(q.distributorCategoryIdRaw())) {
			try {
				f.setDistributorCategoryIdEq(Long.parseLong(q.distributorCategoryIdRaw().trim()));
			} catch (NumberFormatException ignored) {
				// PHP 会把非数字透传进 filter；Java 侧无效值忽略以保持可运行
			}
		}

		// 对齐 PHP：未传 is_valid 时默认 ['true','false']；显式传入则精确匹配
		if (StringUtils.hasText(q.isValidRaw())) {
			f.setIsValid(q.isValidRaw().trim());
			f.setIsValidIn(null);
		} else {
			f.setIsValidIn(List.of("true", "false"));
			f.setIsValid(null);
		}

		boolean isNs = isNostoresTruthy(q.isNostoresRaw());
		if (isNs) {
			f.setIsZiti(Integer.valueOf(1));
		}

		boolean getShopTruthy =
				StringUtils.hasText(q.getShopRaw())
						&& !"0".equalsIgnoreCase(q.getShopRaw().trim())
						&& !"false".equalsIgnoreCase(q.getShopRaw().trim());
		f.setRequireShopIdNonEmpty(getShopTruthy);

		if (StringUtils.hasText(q.cardId())) {
			try {
				long cid = Long.parseLong(q.cardId().trim());
				List<Long> cardDids = discountCardDistributorIdsJdbcRepository.listDistributorIdsByCompanyAndCardId(companyId, cid);
				if (!cardDids.isEmpty()) {
					f.setDistributorIdInList(cardDids);
				}
			} catch (NumberFormatException ignored) {
				// ignore invalid card id
			}
		}

		if (isNs) {
			DistributorWxappShopListFilter scoped = f.copy();
			scoped.setUserLng(null);
			scoped.setUserLat(null);
			scoped.setGeoEnabled(false);
			scoped.setNoHaving(false);
			scoped.setIsZiti(Integer.valueOf(1));
			List<Long> nostoresIds =
					distributorNostoresCartDistributorIdsReadService.listDistributorIdsByNostoresCart(
							companyId,
							userId,
							NostoresScopedDistributorFilterFactory.fromWxappShopListFilter(scoped),
							q.cartType(),
							q.orderType(),
							q.seckillId(),
							q.seckillTicket(),
							q.iscrossborder(),
							q.bargainId());
			if (nostoresIds.isEmpty()) {
				return new DistributorWxappShopListFilterBuildResult(f, isRecommend, true);
			}
			f.setDistributorIdInList(new ArrayList<>(nostoresIds));
		}

		List<Long> tagIds = parseCommaLongs(q.distributorTagIdRaw());
		if (!tagIds.isEmpty()) {
			List<Long> existingList = f.getDistributorIdInList();
			List<Long> scope =
					existingList != null && !existingList.isEmpty() ? existingList : null;
			List<Long> merged =
					distributorTagRelQueryService.listDistributorIdsByCompanyAndTagIds(companyId, tagIds, scope);
			if (merged.isEmpty()) {
				return new DistributorWxappShopListFilterBuildResult(f, isRecommend, true);
			}
			f.setDistributorIdInList(merged);
		}

		// 对齐 PHP exclude_distributor_id：有 IN 列表则剔除；否则 NOT IN / neq
		long excludeId = parsePositiveLong(q.excludeDistributorIdRaw());
		if (excludeId > 0L) {
			List<Long> inList = f.getDistributorIdInList();
			if (inList != null && !inList.isEmpty()) {
				List<Long> next = new ArrayList<>();
				for (Long id : inList) {
					if (id != null && id.longValue() != excludeId) {
						next.add(id);
					}
				}
				if (next.isEmpty()) {
					return new DistributorWxappShopListFilterBuildResult(f, isRecommend, true);
				}
				f.setDistributorIdInList(next);
			} else {
				f.setExcludeDistributorIdList(new ArrayList<>(List.of(Long.valueOf(excludeId))));
			}
		}

		return new DistributorWxappShopListFilterBuildResult(f, isRecommend, false);
	}

	private static long parsePositiveLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static void parseDistributorIdConstraints(DistributorWxappShopListFilter f, DistributorWxappShopListQuery q) {
		if (StringUtils.hasText(q.distributorIdRaw())) {
			String t = q.distributorIdRaw().trim();
			if (!"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				try {
					f.setDistributorIdInList(List.of(Long.parseLong(t)));
				} catch (NumberFormatException ignored) {
					// leave unset
				}
			}
		} else if (q.distributorIds() != null && !q.distributorIds().isEmpty()) {
			Set<Long> ids = new LinkedHashSet<>();
			for (String s : q.distributorIds()) {
				if (!StringUtils.hasText(s)) {
					continue;
				}
				try {
					ids.add(Long.parseLong(s.trim()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
			if (!ids.isEmpty()) {
				f.setDistributorIdInList(new ArrayList<>(ids));
			}
		}
	}

	private void applyType1(
			DistributorWxappShopListFilter f, DistributorWxappShopListQuery q, java.util.Map<String, Object> defaultAddressRowOrNull) {
		f.setProvinceLikeEscaped(escapedLikeOrNull(q.province()));
		f.setCityLikeEscaped(escapedLikeOrNull(q.city()));
		f.setAreaLikeEscaped(escapedLikeOrNull(stripDistrictSuffix(q.area())));
		if (StringUtils.hasText(q.lng()) && StringUtils.hasText(q.lat())) {
			f.setUserLng(q.lng().trim());
			f.setUserLat(q.lat().trim());
		} else if (defaultAddressRowOrNull != null) {
			applyResolvedCoords(f, resolveLngLatFromAddress(f.getCompanyId(), defaultAddressRowOrNull));
		}
	}

	private void applyType2(DistributorWxappShopListFilter f, java.util.Map<String, Object> defaultAddressRowOrNull, long companyId) {
		if (defaultAddressRowOrNull == null) {
			return;
		}
		applyResolvedCoords(f, resolveLngLatFromAddress(companyId, defaultAddressRowOrNull));
	}

	private void applyType3(DistributorWxappShopListFilter f, DistributorWxappShopListQuery q, long companyId) {
		f.setProvinceLikeEscaped(escapedLikeOrNull(q.province()));
		f.setCityLikeEscaped(escapedLikeOrNull(q.city()));
		f.setAreaLikeEscaped(escapedLikeOrNull(stripDistrictSuffix(q.area())));
		Map<String, Object> addressRow = new LinkedHashMap<>();
		addressRow.put("city", q.city());
		addressRow.put("adrdetail", q.address());
		applyResolvedCoords(f, resolveLngLatFromAddress(companyId, addressRow));
	}

	private int applyTypeDefault(
			DistributorWxappShopListFilter f,
			DistributorWxappShopListQuery q,
			java.util.Map<String, Object> defaultAddressRowOrNull,
			long companyId) {
		if (StringUtils.hasText(q.lng()) && StringUtils.hasText(q.lat())) {
			f.setUserLng(q.lng().trim());
			f.setUserLat(q.lat().trim());
			return 0;
		}
		if (defaultAddressRowOrNull != null) {
			applyResolvedCoords(f, resolveLngLatFromAddress(companyId, defaultAddressRowOrNull));
			return 0;
		}
		f.setUserLng(null);
		f.setUserLat(null);
		return 1;
	}

	private record ResolvedLngLat(String lng, String lat) {}

	private void applyResolvedCoords(DistributorWxappShopListFilter f, ResolvedLngLat coords) {
		if (coords == null) {
			return;
		}
		f.setUserLng(coords.lng());
		f.setUserLat(coords.lat());
	}

	private ResolvedLngLat resolveLngLatFromAddress(long companyId, Map<String, Object> addressRow) {
		String lat = stringVal(addressRow.get("lat"));
		String lng = stringVal(addressRow.get("lng"));
		if (StringUtils.hasText(lat) && StringUtils.hasText(lng)) {
			return new ResolvedLngLat(lng, lat);
		}
		String city = stringVal(addressRow.get("city"));
		String adrdetail = stringVal(addressRow.get("adrdetail"));
		if (!StringUtils.hasText(city) || !StringUtils.hasText(adrdetail)) {
			return null;
		}
		CompanyMapGeocodePort.GeocodeLatLng geocoded = companyMapGeocodePort.geocodeAllowEmpty(companyId, city, adrdetail);
		if (geocoded == null) {
			return null;
		}
		return new ResolvedLngLat(geocoded.lng(), geocoded.lat());
	}

	private static String stringVal(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private static String stripDistrictSuffix(String area) {
		if (area == null) {
			return null;
		}
		return area.replace("区", "");
	}

	private static String escapedLikeOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		return DistributorListQueryService.escapeSqlLike(t);
	}

	private static boolean isNostoresTruthy(String raw) {
		return StringUtils.hasText(raw) && !"0".equals(raw.trim());
	}

	private static List<Long> parseCommaLongs(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		Set<Long> out = new LinkedHashSet<>();
		for (String p : raw.split(",")) {
			String t = p.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return new ArrayList<>(out);
	}
}
