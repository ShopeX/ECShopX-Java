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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappItemsListSalesmanDistributorResolveService {

	private static final Logger log = LoggerFactory.getLogger(WxappItemsListSalesmanDistributorResolveService.class);

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final DistributorMapper distributorMapper;

	public WxappItemsListSalesmanDistributorResolveService(ShopSalespersonMapper shopSalespersonMapper, DistributorMapper distributorMapper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.distributorMapper = distributorMapper;
	}

	/**
	 * 店铺推广分类：业务员场景下无显式店铺维度时跳过平台 JWT 分销商合并；无可用分销商时不中断接口。
	 */
	public static record PromoterCategoryDistributorResolution(
			boolean skipPlatformJwtDistributorMerge,
			boolean hasExplicitDistributorFilter,
			Long singleDistributorId,
			List<Long> distributorIdIn) {
	}

	public PromoterCategoryDistributorResolution resolvePromoterCategoryDistributorFilter(
			long companyId, long userId, String isSalesmanPageRaw, String distributorIdRaw) {
		if (!isPromoterSalesmanPageRawTruthy(isSalesmanPageRaw)) {
			Long single = parseSinglePositiveDistributorId(distributorIdRaw);
			return new PromoterCategoryDistributorResolution(false, single != null, single, null);
		}

		LambdaQueryWrapper<ShopSalesperson> sw = new LambdaQueryWrapper<>();
		sw.eq(ShopSalesperson::getCompanyId, companyId).apply("user_id = {0}", userId).eq(ShopSalesperson::getIsValid, "true");
		List<ShopSalesperson> salesRows = shopSalespersonMapper.selectList(sw);
		List<Long> candidateShopIds = new ArrayList<>();
		for (ShopSalesperson s : salesRows) {
			if (s.getShopId() == null) {
				continue;
			}
			try {
				long sid = Long.parseLong(s.getShopId().trim());
				if (sid > 0L) {
					candidateShopIds.add(sid);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		if (!candidateShopIds.isEmpty() && log.isInfoEnabled()) {
			log.info("promoter category salesman branch companyId={} candidateShopCount={}", companyId, candidateShopIds.size());
		}
		if (candidateShopIds.isEmpty()) {
			return new PromoterCategoryDistributorResolution(true, false, null, null);
		}

		LambdaQueryWrapper<Distributor> dw = new LambdaQueryWrapper<>();
		dw.eq(Distributor::getCompanyId, companyId).eq(Distributor::getIsOpenSalesman, true).in(Distributor::getDistributorId, candidateShopIds)
				.orderByDesc(Distributor::getDistributorId).last("LIMIT 100");
		List<Distributor> distRows = distributorMapper.selectList(dw);
		List<Long> openDistIds = distRows.stream().map(Distributor::getDistributorId).filter(id -> id != null && id > 0L).collect(Collectors.toList());
		if (openDistIds.isEmpty()) {
			return new PromoterCategoryDistributorResolution(true, false, null, null);
		}

		Long requested = parseSinglePositiveDistributorId(distributorIdRaw);
		if (requested != null && openDistIds.contains(requested)) {
			return new PromoterCategoryDistributorResolution(false, true, requested, null);
		}
		return new PromoterCategoryDistributorResolution(false, true, null, new ArrayList<>(openDistIds));
	}

	/**
	 * 小店上架类目：当前登录会员在门店人员表中的 {@code shop_id}（分销商维度），与导购列表结果列 {@code shop_id} 一致，最多 {@code maxRows} 条。
	 */
	public List<Long> listRawSalespersonShopIdsForWxappUser(long companyId, long userId, int maxRows) {
		if (companyId <= 0L || userId <= 0L || maxRows <= 0) {
			return List.of();
		}
		int cap = Math.min(maxRows, 500);
		LambdaQueryWrapper<ShopSalesperson> sw = new LambdaQueryWrapper<>();
		sw.eq(ShopSalesperson::getCompanyId, companyId).apply("user_id = {0}", userId).eq(ShopSalesperson::getIsValid, "true")
				.last("LIMIT " + cap);
		List<ShopSalesperson> salesRows = shopSalespersonMapper.selectList(sw);
		LinkedHashSet<Long> ordered = new LinkedHashSet<>();
		for (ShopSalesperson s : salesRows) {
			if (s.getShopId() == null) {
				continue;
			}
			try {
				long sid = Long.parseLong(s.getShopId().trim());
				if (sid > 0L) {
					ordered.add(sid);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return new ArrayList<>(ordered);
	}

	private static boolean isPromoterSalesmanPageRawTruthy(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return false;
		}
		String t = raw.trim();
		if ("1".equals(t) || "true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t)) {
			return true;
		}
		try {
			return Integer.parseInt(t) != 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static Long parseSinglePositiveDistributorId(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		String t = raw.trim();
		if (t.contains(",")) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public WxappItemsListSalesmanGateResult resolve(long companyId, long userId, LinkedHashMap<String, Object> params) {
		String flag = stringParam(params.get("isSalesmanPage"));
		if (!"1".equals(flag != null ? flag.trim() : "")) {
			return WxappItemsListSalesmanGateResult.proceed(new LinkedHashMap<>());
		}
		if (userId <= 0L) {
			LinkedHashMap<String, Object> patch = new LinkedHashMap<>();
			patch.put("distributor_id", "");
			return WxappItemsListSalesmanGateResult.proceed(patch);
		}
		String storeStatus = stringParam(params.get("store_status"));
		LinkedHashMap<String, Object> prePatch = new LinkedHashMap<>();
		if (StringUtils.hasText(storeStatus)) {
			if (!"0".equals(storeStatus.trim())) {
				prePatch.put("store|gt", 0);
			} else {
				prePatch.put("store|lt", 1);
			}
		}
		prePatch.put("rebate", 1);
		prePatch.put("item_type", "normal");

		LambdaQueryWrapper<ShopSalesperson> sw = new LambdaQueryWrapper<>();
		sw.eq(ShopSalesperson::getCompanyId, companyId).apply("user_id = {0}", userId).eq(ShopSalesperson::getIsValid, "true");
		List<ShopSalesperson> salesRows = shopSalespersonMapper.selectList(sw);
		List<Long> candidateShopIds = new ArrayList<>();
		for (ShopSalesperson s : salesRows) {
			if (s.getShopId() == null) {
				continue;
			}
			try {
				long sid = Long.parseLong(s.getShopId().trim());
				if (sid > 0L) {
					candidateShopIds.add(sid);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		if (candidateShopIds.isEmpty()) {
			return WxappItemsListSalesmanGateResult.empty(1);
		}

		String reqDist = distributorRequestToken(params.get("distributor_id"));
		List<Long> dFilterIds = new ArrayList<>(new LinkedHashSet<>(candidateShopIds));
		if (StringUtils.hasText(reqDist) && !"false".equalsIgnoreCase(reqDist.trim())) {
			try {
				long single = Long.parseLong(reqDist.trim().split(",")[0]);
				if (candidateShopIds.contains(single)) {
					dFilterIds = List.of(single);
				}
			} catch (NumberFormatException ignored) {
				// keep full candidate list
			}
		}
		if (dFilterIds.isEmpty()) {
			return WxappItemsListSalesmanGateResult.empty(1);
		}

		LambdaQueryWrapper<Distributor> dw = new LambdaQueryWrapper<>();
		dw.eq(Distributor::getCompanyId, companyId).eq(Distributor::getIsOpenSalesman, true).in(Distributor::getDistributorId, dFilterIds)
				.orderByDesc(Distributor::getDistributorId).last("LIMIT 100");
		List<Distributor> distRows = distributorMapper.selectList(dw);
		List<Long> openDistIds = distRows.stream().map(Distributor::getDistributorId).filter(id -> id != null && id > 0L).collect(Collectors.toList());
		if (openDistIds.isEmpty()) {
			return WxappItemsListSalesmanGateResult.empty(2);
		}

		Object originalDist = params.get("distributor_id");
		LinkedHashMap<String, Object> patch = new LinkedHashMap<>(prePatch);
		Long singleRequested = extractSingleRequestedDistributorId(originalDist);
		if (singleRequested != null) {
			if (openDistIds.contains(singleRequested)) {
				patch.put("distributor_id", singleRequested);
			} else {
				return WxappItemsListSalesmanGateResult.empty(2);
			}
		} else {
			patch.put("distributor_id", openDistIds);
		}
		return WxappItemsListSalesmanGateResult.proceed(patch);
	}

	private static Long extractSingleRequestedDistributorId(Object originalDist) {
		if (originalDist == null) {
			return null;
		}
		if (originalDist instanceof List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			if (first instanceof Number n) {
				long v = n.longValue();
				return v >= 0L ? v : null;
			}
			return parseLongOrNull(first != null ? first.toString() : "");
		}
		String s = originalDist.toString().trim();
		if (s.isEmpty() || "false".equalsIgnoreCase(s)) {
			return null;
		}
		if (s.contains(",")) {
			return null;
		}
		return parseLongOrNull(s);
	}

	private static Long parseLongOrNull(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String stringParam(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return raw.toString();
	}

	private static String distributorRequestToken(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			return first != null ? first.toString() : null;
		}
		return raw.toString();
	}
}
