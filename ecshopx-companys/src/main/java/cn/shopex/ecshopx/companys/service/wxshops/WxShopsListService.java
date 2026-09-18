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

package cn.shopex.ecshopx.companys.service.wxshops;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxShopsListService {

	private final WxShopsMapper wxShopsMapper;
	private final WxShopsJwtShopIdWhitelist wxShopsJwtShopIdWhitelist;
	private final ObjectMapper objectMapper;

	public WxShopsListService(
			WxShopsMapper wxShopsMapper,
			WxShopsJwtShopIdWhitelist wxShopsJwtShopIdWhitelist,
			ObjectMapper objectMapper) {
		this.wxShopsMapper = wxShopsMapper;
		this.wxShopsJwtShopIdWhitelist = wxShopsJwtShopIdWhitelist;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getWxShopsList(
			long companyId,
			Map<String, Object> operatorJwt,
			int page,
			Integer pageSize,
			String isValid,
			String isValidUnderscore,
			String name,
			Long distributorIdInput,
			String province,
			String city,
			String area,
			String poiId,
			String wxShopIdQueryRaw,
			String isDirectStoreRaw) {
		int p = page < 1 ? 1 : page;
		int ps = (pageSize == null || pageSize == 0) ? 100 : pageSize;
		long nowSec = Instant.now().getEpochSecond();

		LambdaQueryWrapper<WxShops> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WxShops::getCompanyId, companyId);

		if ("true".equals(isValid) || "true".equals(isValidUnderscore)) {
			wrapper.gt(WxShops::getExpiredAt, nowSec);
		}

		if (name != null && !name.isBlank()) {
			wrapper.like(WxShops::getStoreName, "%" + escapeLike(name.trim()) + "%");
		}

		long resolvedDistributorId = distributorIdInput != null ? distributorIdInput : 0L;
		wrapper.eq(WxShops::getDistributorId, resolvedDistributorId);

		List<String> addrParts = new ArrayList<>();
		if (province != null && !province.isBlank()) {
			addrParts.add(province);
		}
		if (city != null && !city.isBlank()) {
			addrParts.add(city);
		}
		if (area != null && !area.isBlank()) {
			addrParts.add(area);
		}
		for (String part : addrParts) {
			wrapper.like(WxShops::getAddress, "%" + escapeLike(part.trim()) + "%");
		}

		if (poiId != null && !poiId.isBlank()) {
			wrapper.eq(WxShops::getPoiId, poiId);
		}

		List<Long> jwtShopIds = wxShopsJwtShopIdWhitelist.allowedShopIds(operatorJwt.get("shop_ids"));
		if (truthyRequestInput(wxShopIdQueryRaw)) {
			long wxShopId = parseLongStrictOrThrow(wxShopIdQueryRaw, "参数错误");
			wrapper.eq(WxShops::getWxShopId, wxShopId);
		} else if (!jwtShopIds.isEmpty()) {
			wrapper.in(WxShops::getWxShopId, jwtShopIds);
		}

		if (truthyRequestInput(isDirectStoreRaw)) {
			int direct = parseIntegerStrictOrThrow(isDirectStoreRaw, "参数错误");
			wrapper.eq(WxShops::getIsDirectStore, direct);
		}

		wrapper.orderByDesc(WxShops::getWxShopId);

		Page<WxShops> pageReq = new Page<>(p, ps);
		Page<WxShops> resultPage = wxShopsMapper.selectPage(pageReq, wrapper);
		long total = resultPage.getTotal();

		List<Map<String, Object>> listOfMaps = new ArrayList<>();
		for (WxShops e : resultPage.getRecords()) {
			listOfMaps.add(toNormalizedListRow(e, nowSec));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", listOfMaps);
		return data;
	}

	public Map<String, Object> toNormalizedListRowForReuse(WxShops e, long nowSec) {
		return toNormalizedListRow(e, nowSec);
	}

	private Map<String, Object> toNormalizedListRow(WxShops e, long nowSec) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("wxShopId", e.getWxShopId());
		row.put("mapPoiId", e.getMapPoiId());
		row.put("storeName", e.getStoreName());
		row.put("poiId", e.getPoiId());
		row.put("lng", e.getLng());
		row.put("lat", e.getLat());
		row.put("address", e.getAddress());
		row.put("category", e.getCategory());
		row.put("distributorId", e.getDistributorId());
		row.put("contractPhone", e.getContractPhone());
		row.put("hour", e.getHour());
		row.put("addType", e.getAddType());
		row.put("credential", e.getCredential());
		row.put("companyName", e.getCompanyName());
		row.put("qualificationList", e.getQualificationList());
		row.put("cardId", e.getCardId());
		row.put("status", e.getStatus());
		row.put("errmsg", e.getErrmsg());
		row.put("companyId", e.getCompanyId());
		row.put("auditId", e.getAuditId());
		row.put("resourceId", e.getResourceId());
		row.put("expiredAt", e.getExpiredAt());
		row.put("isDefault", e.getIsDefault());
		row.put("country", e.getCountry());
		row.put("city", e.getCity());
		row.put("isDomestic", e.getIsDomestic());
		row.put("isDirectStore", e.getIsDirectStore());
		row.put("isOpen", e.getIsOpen());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());

		String rawPic = e.getPicList();
		if (rawPic == null || rawPic.isBlank()) {
			row.put("picList", null);
		} else {
			try {
				row.put("picList", objectMapper.readValue(rawPic, Object.class));
			} catch (JsonProcessingException ex) {
				row.put("picList", Collections.emptyList());
			}
		}

		Long exp = e.getExpiredAt();
		boolean isValidFlag = exp != null && exp >= nowSec;
		row.put("is_valid", isValidFlag);
		return row;
	}

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}

	private static boolean truthyRequestInput(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		return false;
	}

	private static long parseLongStrictOrThrow(Object raw, String message) {
		try {
			if (raw instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
	}

	private static int parseIntegerStrictOrThrow(Object raw, String message) {
		try {
			if (raw instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
	}
}
