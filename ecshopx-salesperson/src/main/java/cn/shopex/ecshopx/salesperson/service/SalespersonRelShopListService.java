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

import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingRedisReadService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonRelShopListService {

	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final WxShopsMapper wxShopsMapper;
	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorMapper distributorMapper;

	public SalespersonRelShopListService(ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			WxShopsMapper wxShopsMapper,
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorMapper distributorMapper) {
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.wxShopsMapper = wxShopsMapper;
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorMapper = distributorMapper;
	}

	public Map<String, Object> getRelShopList(long companyId, String salespersonIdRaw, String storeTypeRaw,
			String storeNameRaw, int page, int pageSize) {
		String r2 = (storeTypeRaw == null) ? "" : storeTypeRaw.trim();
		String normalizedStoreType = r2.isEmpty() ? "shop" : r2;
		return buildRelShopList(companyId, salespersonIdRaw, storeNameRaw, page, pageSize, normalizedStoreType,
				true);
	}

	public Map<String, Object> getDistributorDataList(long companyId, String salespersonIdRaw,
			String storeTypeRaw, String storeNameRaw, int page, int pageSize) {
		String r = (storeTypeRaw == null) ? "" : storeTypeRaw.trim();
		String normalizedStoreType = r.isEmpty() ? "distributor" : r;
		return buildRelShopList(companyId, salespersonIdRaw, storeNameRaw, page, pageSize, normalizedStoreType,
				false);
	}

	private Map<String, Object> buildRelShopList(long companyId, String salespersonIdRaw,
			String storeNameRaw, int page, int pageSize,
			String normalizedStoreType, boolean includeTagname) {
		int pageNorm = page < 1 ? 1 : page;

		Long salespersonId = parsePositiveSalespersonId(salespersonIdRaw);
		if (salespersonId == null) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		LambdaQueryWrapper<ShopsRelSalesperson> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ShopsRelSalesperson::getCompanyId, companyId);
		wrapper.eq(ShopsRelSalesperson::getStoreType, normalizedStoreType);
		wrapper.eq(ShopsRelSalesperson::getSalespersonId, salespersonId);

		String nameFilter = (storeNameRaw == null) ? "" : storeNameRaw.trim();
		if (StringUtils.hasText(nameFilter)) {
			String escaped = escapeSqlLike(nameFilter);
			List<Distributor> matched = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.like(Distributor::getName, "%" + escaped + "%"));
			List<Long> filterShopIds;
			if (matched.isEmpty()) {
				filterShopIds = List.of(0L);
			} else {
				filterShopIds = new ArrayList<>(matched.size());
				for (Distributor d : matched) {
					if (d.getDistributorId() != null) {
						filterShopIds.add(d.getDistributorId());
					}
				}
				if (filterShopIds.isEmpty()) {
					filterShopIds = List.of(0L);
				}
			}
			wrapper.in(ShopsRelSalesperson::getShopId, filterShopIds);
		}

		long totalCount = shopsRelSalespersonMapper.selectCount(wrapper);

		List<ShopsRelSalesperson> list;
		if (pageSize > 0) {
			Page<ShopsRelSalesperson> p = new Page<>(pageNorm, pageSize, false);
			shopsRelSalespersonMapper.selectPage(p, wrapper);
			list = p.getRecords();
		} else {
			list = shopsRelSalespersonMapper.selectList(wrapper);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		if (list.isEmpty()) {
			out.put("list", List.of());
			return out;
		}

		LinkedHashSet<Long> shopWxIds = new LinkedHashSet<>();
		LinkedHashSet<Long> distributorIds = new LinkedHashSet<>();
		for (ShopsRelSalesperson rel : list) {
			if (rel.getShopId() == null) {
				continue;
			}
			if ("shop".equals(rel.getStoreType())) {
				shopWxIds.add(rel.getShopId());
			} else if ("distributor".equals(rel.getStoreType())) {
				distributorIds.add(rel.getShopId());
			}
		}

		Map<Long, Map<String, Object>> shopRowByWxId = new LinkedHashMap<>();
		if (!shopWxIds.isEmpty()) {
			Map<String, Object> wxSetting = wxShopsSettingRedisReadService.load(companyId);
			Object logoObj = wxSetting.get("logo");
			String unifiedLogo = logoObj == null ? null : String.valueOf(logoObj);
			List<WxShops> wxRows = wxShopsMapper.selectList(new LambdaQueryWrapper<WxShops>()
					.eq(WxShops::getCompanyId, companyId)
					.in(WxShops::getWxShopId, shopWxIds));
			for (WxShops ws : wxRows) {
				if (ws.getWxShopId() == null) {
					continue;
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("address", ws.getAddress());
				row.put("name", ws.getStoreName());
				row.put("shop_id", ws.getWxShopId());
				row.put("logo", unifiedLogo);
				row.put("distributor_id", ws.getDistributorId());
				shopRowByWxId.put(ws.getWxShopId(), row);
			}
		}

		Map<Long, Map<String, Object>> distRowById = new LinkedHashMap<>();
		if (!distributorIds.isEmpty()) {
			List<Long> distIdList = new ArrayList<>(distributorIds);
			List<Map<String, Object>> easyRows =
					distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId, distIdList);
			for (Map<String, Object> row : easyRows) {
				if (row == null) {
					continue;
				}
				Object did = row.get("distributor_id");
				long id = longFromDistributorId(did);
				if (id >= 0L) {
					distRowById.put(id, row);
				}
			}
		}

		List<Map<String, Object>> rowsOut = new ArrayList<>(list.size());
		for (ShopsRelSalesperson rel : list) {
			rowsOut.add(toOutputRow(rel, shopRowByWxId, distRowById, includeTagname));
		}
		out.put("list", rowsOut);
		return out;
	}

	private Map<String, Object> toOutputRow(ShopsRelSalesperson rel,
			Map<Long, Map<String, Object>> shopRowByWxId,
			Map<Long, Map<String, Object>> distRowById,
			boolean includeTagname) {
		Map<String, Object> shop;
		if ("shop".equals(rel.getStoreType()) && rel.getShopId() != null) {
			shop = shopRowByWxId.getOrDefault(rel.getShopId(), Map.of());
		} else if ("distributor".equals(rel.getStoreType()) && rel.getShopId() != null) {
			shop = distRowById.getOrDefault(rel.getShopId(), Map.of());
		} else {
			shop = Map.of();
		}

		String address = textOrUnknown(shop.get("address"));
		String storeName = textOrUnknown(shop.get("name"));

		String displayShopId;
		if ("shop".equals(rel.getStoreType())) {
			Object sid = shop.get("shop_id");
			displayShopId = sid == null ? "未知" : String.valueOf(sid);
		} else {
			Object did = shop.get("distributor_id");
			displayShopId = did == null ? "未知" : String.valueOf(did);
		}

		String distributorIdStr = stringIdOrUnknown(shop.get("distributor_id"));
		String shopLogo = stringLogoOrUnknown(shop.get("logo"));

		String hour;
		if ("shop".equals(rel.getStoreType())) {
			hour = "未知";
		} else {
			Object h = shop.get("hour");
			hour = h == null || !StringUtils.hasText(String.valueOf(h).trim()) ? "未知" : String.valueOf(h);
		}

		Map<String, Object> row = new LinkedHashMap<>();
		if (includeTagname) {
			String tagname = "shop".equals(rel.getStoreType()) ? "门店" : "店铺";
			row.put("tagname", tagname);
		}
		row.put("shop_id", displayShopId);
		row.put("salesperson_id", rel.getSalespersonId() == null ? "未知" : String.valueOf(rel.getSalespersonId()));
		row.put("company_id", rel.getCompanyId() == null ? "未知" : String.valueOf(rel.getCompanyId()));
		row.put("store_type", rel.getStoreType() != null ? rel.getStoreType() : "");
		row.put("address", address);
		row.put("store_name", storeName);
		row.put("distributor_id", distributorIdStr);
		row.put("shop_logo", shopLogo);
		row.put("hour", hour);
		return row;
	}

	private static String textOrUnknown(Object raw) {
		if (raw == null) {
			return "未知";
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : "未知";
	}

	private static String stringIdOrUnknown(Object raw) {
		if (raw == null) {
			return "未知";
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : "未知";
	}

	private static String stringLogoOrUnknown(Object raw) {
		if (raw == null) {
			return "未知";
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : "未知";
	}

	private static Long parsePositiveSalespersonId(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		long v;
		try {
			v = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
		if (v <= 0L) {
			return null;
		}
		return v;
	}

	private static long longFromDistributorId(Object raw) {
		if (raw == null) {
			return -1L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	private static String escapeSqlLike(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
