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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxShopsListForUserDiscountService {

	public static final int PAGE = 1;
	public static final int PAGE_SIZE = 50;

	private final WxShopsMapper wxShopsMapper;
	private final ObjectMapper objectMapper;

	public WxShopsListForUserDiscountService(WxShopsMapper wxShopsMapper, ObjectMapper objectMapper) {
		this.wxShopsMapper = wxShopsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> listShopsPoi(long companyId, Object relShopsIds) {
		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<WxShops> countQ = baseQuery(companyId, nowSec, relShopsIds);
		long total = wxShopsMapper.selectCount(countQ);

		LambdaQueryWrapper<WxShops> listQ = baseQuery(companyId, nowSec, relShopsIds);
		listQ.orderByDesc(WxShops::getWxShopId);
		listQ.last("LIMIT " + PAGE_SIZE);
		List<WxShops> rows = wxShopsMapper.selectList(listQ);
		List<Map<String, Object>> list = new ArrayList<>();
		for (WxShops w : rows) {
			list.add(toShopEntry(w, nowSec));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total", total);
		out.put("page", PAGE);
		out.put("pageSize", PAGE_SIZE);
		out.put("total_count", total);
		return out;
	}

	private static LambdaQueryWrapper<WxShops> baseQuery(long companyId, long nowSec, Object relShopsIds) {
		LambdaQueryWrapper<WxShops> q = new LambdaQueryWrapper<WxShops>()
				.eq(WxShops::getCompanyId, companyId)
				.gt(WxShops::getExpiredAt, nowSec);
		if (!isAllShops(relShopsIds)) {
			List<Long> ids = normalizeWxShopIds(relShopsIds);
			if (ids.isEmpty()) {
				q.eq(WxShops::getWxShopId, -1L);
			} else {
				q.in(WxShops::getWxShopId, ids);
			}
		}
		return q;
	}

	private Map<String, Object> toShopEntry(WxShops w, long nowSec) {
		Map<String, Object> m = new LinkedHashMap<>();
		Long wxId = w.getWxShopId();
		m.put("wxShopId", wxId);
		m.put("companyName", nullToEmpty(w.getCompanyName()));
		m.put("storeName", nullToEmpty(w.getStoreName()));
		m.put("picList", parsePicList(w.getPicList()));
		Long exp = w.getExpiredAt();
		boolean valid = exp != null && exp >= nowSec;
		m.put("is_valid", valid);
		m.put("expiredAt", exp == null ? 0L : exp);
		m.put("lng", w.getLng());
		m.put("lat", w.getLat());
		m.put("address", w.getAddress());
		m.put("contractPhone", w.getContractPhone());
		m.put("hour", w.getHour());
		m.put("mapPoiId", w.getMapPoiId());
		m.put("poiId", w.getPoiId());
		m.put("category", w.getCategory());
		m.put("distributorId", w.getDistributorId());
		m.put("addType", w.getAddType());
		m.put("status", w.getStatus());
		m.put("errmsg", w.getErrmsg());
		m.put("companyId", w.getCompanyId());
		m.put("auditId", w.getAuditId());
		m.put("resourceId", w.getResourceId());
		m.put("isDefault", w.getIsDefault());
		m.put("country", w.getCountry());
		m.put("city", w.getCity());
		m.put("isDomestic", w.getIsDomestic());
		m.put("isDirectStore", w.getIsDirectStore());
		m.put("isOpen", w.getIsOpen());
		m.put("created", w.getCreated());
		m.put("updated", w.getUpdated());
		return m;
	}

	private List<Object> parsePicList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(raw.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private static boolean isAllShops(Object relShopsIds) {
		if (relShopsIds == null) {
			return true;
		}
		if (relShopsIds instanceof String s) {
			return "all".equalsIgnoreCase(s.trim());
		}
		return false;
	}

	private static List<Long> normalizeWxShopIds(Object relShopsIds) {
		List<Long> out = new ArrayList<>();
		if (!(relShopsIds instanceof List<?> list)) {
			return out;
		}
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			if (o instanceof Number n) {
				long v = n.longValue();
				if (v > 0L) {
					out.add(v);
				}
				continue;
			}
			String s = o.toString().trim();
			if (StringUtils.hasText(s)) {
				try {
					long v = Long.parseLong(s);
					if (v > 0L) {
						out.add(v);
					}
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return out;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
