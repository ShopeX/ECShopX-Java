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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappShopListRepository;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidSettingService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListAppendBlocksService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListFilterBuildService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListRowAssemblyService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListSalesNetRankingService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.DistributorWxappShopListTagMetadataReadService;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListFilterBuildResult;
import cn.shopex.ecshopx.distribution.service.wxappdistributorlist.dto.DistributorWxappShopListQuery;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.address.MemberAddressDefaultReadService;
import cn.shopex.ecshopx.members.service.address.MemberAddressLatLngNonNumericEnqueueService;
import cn.shopex.ecshopx.merchant.service.MerchantDisabledDistributorIdsQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * After the default member address snapshot is attached to the H5 list payload, eligible rows may hand
 * off to {@link MemberAddressLatLngNonNumericEnqueueService} for a deferred coordinate refresh when the
 * signed-in member id is positive.
 */
@Service
public class DistributorH5GetDistributorListService {

	private final DistributorWxappShopListTagMetadataReadService distributorWxappShopListTagMetadataReadService;
	private final MemberAddressDefaultReadService memberAddressDefaultReadService;
	private final MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService;
	private final DistributorWxappShopListFilterBuildService distributorWxappShopListFilterBuildService;
	private final DistributorWxappShopListSalesNetRankingService distributorWxappShopListSalesNetRankingService;
	private final MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService;
	private final DistributorIsValidSettingService distributorIsValidSettingService;
	private final DistributorWhiteListMapper distributorWhiteListMapper;
	private final MemberAccountService memberAccountService;
	private final DistributorWxappShopListRepository distributorWxappShopListRepository;
	private final DistributorMapper distributorMapper;
	private final DistributorWxappShopListRowAssemblyService distributorWxappShopListRowAssemblyService;
	private final DistributorWxappShopListAppendBlocksService distributorWxappShopListAppendBlocksService;
	private final DistributorCategoryService distributorCategoryService;

	public DistributorH5GetDistributorListService(
			DistributorWxappShopListTagMetadataReadService distributorWxappShopListTagMetadataReadService,
			MemberAddressDefaultReadService memberAddressDefaultReadService,
			MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService,
			DistributorWxappShopListFilterBuildService distributorWxappShopListFilterBuildService,
			DistributorWxappShopListSalesNetRankingService distributorWxappShopListSalesNetRankingService,
			MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService,
			DistributorIsValidSettingService distributorIsValidSettingService,
			DistributorWhiteListMapper distributorWhiteListMapper,
			MemberAccountService memberAccountService,
			DistributorWxappShopListRepository distributorWxappShopListRepository,
			DistributorMapper distributorMapper,
			DistributorWxappShopListRowAssemblyService distributorWxappShopListRowAssemblyService,
			DistributorWxappShopListAppendBlocksService distributorWxappShopListAppendBlocksService,
			DistributorCategoryService distributorCategoryService) {
		this.distributorWxappShopListTagMetadataReadService = distributorWxappShopListTagMetadataReadService;
		this.memberAddressDefaultReadService = memberAddressDefaultReadService;
		this.memberAddressLatLngNonNumericEnqueueService = memberAddressLatLngNonNumericEnqueueService;
		this.distributorWxappShopListFilterBuildService = distributorWxappShopListFilterBuildService;
		this.distributorWxappShopListSalesNetRankingService = distributorWxappShopListSalesNetRankingService;
		this.merchantDisabledDistributorIdsQueryService = merchantDisabledDistributorIdsQueryService;
		this.distributorIsValidSettingService = distributorIsValidSettingService;
		this.distributorWhiteListMapper = distributorWhiteListMapper;
		this.memberAccountService = memberAccountService;
		this.distributorWxappShopListRepository = distributorWxappShopListRepository;
		this.distributorMapper = distributorMapper;
		this.distributorWxappShopListRowAssemblyService = distributorWxappShopListRowAssemblyService;
		this.distributorWxappShopListAppendBlocksService = distributorWxappShopListAppendBlocksService;
		this.distributorCategoryService = distributorCategoryService;
	}

	public Map<String, Object> getDistributorList(long companyId, long userId, DistributorWxappShopListQuery q, String requestLangTag) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", Integer.valueOf(0));
		result.put("list", List.of());
		result.put("tagList", distributorWxappShopListTagMetadataReadService.listFrontShowTagRows(companyId));
		Map<String, Object> defaultAddr = null;
		if (userId > 0L) {
			defaultAddr = memberAddressDefaultReadService.getDefaultAddress(companyId, userId);
		}
		result.put("defualt_address", defaultAddr == null ? Map.of() : defaultAddr);
		result.put("is_recommend", Integer.valueOf(0));
		if (userId > 0L) {
			memberAddressLatLngNonNumericEnqueueService.enqueueIfNeeded(companyId, userId, defaultAddr);
		}

		validateLngLatSometimes(q.lng(), q.lat());

		DistributorWxappShopListFilterBuildResult built =
				distributorWxappShopListFilterBuildService.build(companyId, userId, q, defaultAddr);
		if (built.earlyEmpty()) {
			return finalizeResult(result, built.isRecommend());
		}
		DistributorWxappShopListFilter filter = built.filter();
		result.put("is_recommend", Integer.valueOf(built.isRecommend()));

		List<Long> allCompanyDistributorIds = listAllDistributorIds(companyId);
		applySortFlags(filter, q.sortType(), companyId, allCompanyDistributorIds);

		applyMerchantDisabledExclusions(companyId, filter);

		boolean dividedSelfGeoBypass = false;
		Map<Long, Long> outSortWhitelistRowIdByDistributorId = Map.of();
		Map<String, Object> setting = distributorIsValidSettingService.getOpenDividedSetting(companyId);
		if (Boolean.TRUE.equals(setting.get("status"))) {
			filter.setOpenDivided(Integer.valueOf(0));
			if (StringUtils.hasText(q.showType()) && "self".equals(q.showType())) {
				List<DistributorWhiteList> wlRows = loadWhitelistRowsForUser(companyId, userId);
				if (wlRows.isEmpty()) {
					return finalizeResult(result, built.isRecommend());
				}
				List<Long> whiteIds =
						wlRows.stream().map(DistributorWhiteList::getDistributorId).distinct().collect(Collectors.toList());
				boolean rqGeo = StringUtils.hasText(q.lng()) && StringUtils.hasText(q.lat());
				if (!rqGeo) {
					outSortWhitelistRowIdByDistributorId = new LinkedHashMap<>();
					for (DistributorWhiteList w : wlRows) {
						if (w.getDistributorId() != null && w.getId() != null) {
							outSortWhitelistRowIdByDistributorId.put(w.getDistributorId(), w.getId());
						}
					}
				}
				filter.setOpenDivided(Integer.valueOf(1));
				filter.setDistributorIdInList(new ArrayList<>(whiteIds));
				dividedSelfGeoBypass = true;
			}
		}

		boolean isNs = StringUtils.hasText(q.isNostoresRaw()) && !"0".equals(q.isNostoresRaw().trim());
		boolean rqGeo = StringUtils.hasText(q.lng()) && StringUtils.hasText(q.lat());
		filter.setNoHaving(dividedSelfGeoBypass || (isNs && rqGeo));

		filter.setOffset((long) Math.max(0, (q.page() - 1) * q.pageSize()));
		filter.setLimit(Math.max(1, q.pageSize()));

		long nowSec = System.currentTimeMillis() / 1000L;
		Map<Long, Long> netSales = distributorWxappShopListSalesNetRankingService.buildNetSalesMapForCompany(companyId, nowSec);

		long total = distributorWxappShopListRepository.countByFilter(filter);
		List<Distributor> entities = distributorWxappShopListRepository.selectPageByFilter(filter);
		boolean geo = filter.isGeoEnabled();
		List<Map<String, Object>> rows =
				distributorWxappShopListRowAssemblyService.toResponseRows(
						companyId, geo, entities, requestLangTag, outSortWhitelistRowIdByDistributorId);
		distributorCategoryService.appendDistributorCategoryName(companyId, rows);

		List<Long> itemTagIds = parseCommaLongs(q.itemTagIdRaw());
		distributorWxappShopListAppendBlocksService.appendAllConfiguredBlocks(
				companyId,
				q.showTag(),
				q.showDiscount(),
				q.showMarketingActivity(),
				q.showSalesCount(),
				q.showScore(),
				q.showItems(),
				itemTagIds,
				rows,
				nowSec,
				netSales);

		result.put("total_count", Integer.valueOf((int) Math.min(total, Integer.MAX_VALUE)));
		result.put("list", rows);
		return result;
	}

	private Map<String, Object> finalizeResult(Map<String, Object> result, int isRecommend) {
		result.put("is_recommend", Integer.valueOf(isRecommend));
		return result;
	}

	private void applyMerchantDisabledExclusions(long companyId, DistributorWxappShopListFilter filter) {
		List<Long> disabled = merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(companyId);
		if (disabled.isEmpty()) {
			return;
		}
		Set<Long> dis = new HashSet<>(disabled);
		List<Long> cur = filter.getDistributorIdInList();
		if (cur != null && !cur.isEmpty()) {
			List<Long> next = cur.stream().filter(id -> !dis.contains(id)).collect(Collectors.toList());
			filter.setDistributorIdInList(next.isEmpty() ? List.of(-1L) : next);
		} else {
			List<Long> existing = filter.getExcludeDistributorIdList();
			List<Long> merged = new ArrayList<>();
			if (existing != null) {
				merged.addAll(existing);
			}
			for (Long id : disabled) {
				if (id != null && !merged.contains(id)) {
					merged.add(id);
				}
			}
			filter.setExcludeDistributorIdList(merged);
		}
	}

	private void applySortFlags(
			DistributorWxappShopListFilter filter, int sortType, long companyId, List<Long> allCompanyDistributorIds) {
		filter.setOrderByCreatedDescOnly(false);
		filter.setOrderByIsDefaultBranch(false);
		filter.setOrderByDistanceAsc(false);
		filter.setOrderByDistanceDesc(false);
		filter.setUseFieldOrder(false);
		filter.setFieldOrderIds(null);
		boolean hasGeo = filter.isGeoEnabled();
		if (sortType == 1 || sortType == 2) {
			if (hasGeo) {
				if (sortType == 1) {
					filter.setOrderByDistanceAsc(true);
				} else {
					filter.setOrderByDistanceDesc(true);
				}
			} else {
				filter.setOrderByIsDefaultBranch(true);
			}
			return;
		}
		if (sortType == 3 || sortType == 4) {
			List<Long> ordered =
					distributorWxappShopListSalesNetRankingService.buildFieldOrderDistributorIds(
							sortType, companyId, allCompanyDistributorIds, System.currentTimeMillis() / 1000L);
			if (!ordered.isEmpty()) {
				filter.setUseFieldOrder(true);
				filter.setFieldOrderIds(ordered);
			} else {
				filter.setOrderByCreatedDescOnly(true);
			}
			return;
		}
		filter.setOrderByCreatedDescOnly(true);
	}

	private List<Long> listAllDistributorIds(long companyId) {
		List<Distributor> rows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.select(Distributor::getDistributorId));
		List<Long> ids = new ArrayList<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() != null) {
				ids.add(d.getDistributorId());
			}
		}
		return ids;
	}

	private List<DistributorWhiteList> loadWhitelistRowsForUser(long companyId, long userId) {
		if (userId <= 0L) {
			return List.of();
		}
		List<DistributorWhiteList> all =
				distributorWhiteListMapper.selectList(
						new LambdaQueryWrapper<DistributorWhiteList>()
								.eq(DistributorWhiteList::getCompanyId, companyId)
								.orderByDesc(DistributorWhiteList::getCreated));
		if (all.isEmpty()) {
			return List.of();
		}
		String mobile = memberAccountService.findMobileStored(companyId, userId);
		if (!StringUtils.hasText(mobile)) {
			return List.of();
		}
		return all.stream().filter(w -> mobile.equals(w.getMobile())).collect(Collectors.toList());
	}

	private static List<Long> parseCommaLongs(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
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
		return out;
	}

	private static void validateLngLatSometimes(String lngRaw, String latRaw) {
		boolean lngBlank = lngRaw == null || !StringUtils.hasText(lngRaw.trim());
		boolean latBlank = latRaw == null || !StringUtils.hasText(latRaw.trim());
		if (lngBlank && latBlank) {
			return;
		}
		if (lngBlank ^ latBlank) {
			Map<String, List<String>> err = new LinkedHashMap<>();
			if (lngBlank) {
				err.put("lng", List.of("validation.required"));
			}
			if (latBlank) {
				err.put("lat", List.of("validation.required"));
			}
			throw new BadRequestException("经纬度范围错误.", err);
		}
		double lng;
		double lat;
		try {
			lng = Double.parseDouble(lngRaw.trim());
			lat = Double.parseDouble(latRaw.trim());
		} catch (NumberFormatException e) {
			Map<String, List<String>> err = new LinkedHashMap<>();
			err.put("lng", List.of("validation.numeric"));
			err.put("lat", List.of("validation.numeric"));
			throw new BadRequestException("经纬度范围错误.", err);
		}
		if (!Double.isFinite(lng) || !Double.isFinite(lat) || lng < -180.0d || lng > 180.0d || lat < -90.0d || lat > 90.0d) {
			Map<String, List<String>> err = new LinkedHashMap<>();
			err.put("lng", List.of("经度超出允许范围"));
			err.put("lat", List.of("纬度超出允许范围"));
			throw new BadRequestException("经纬度范围错误.", err);
		}
	}
}
