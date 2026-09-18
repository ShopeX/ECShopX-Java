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

import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class H5WxShopsListQueryService {

	private final WxShopsMapper wxShopsMapper;
	private final WxShopsListService wxShopsListService;

	public H5WxShopsListQueryService(WxShopsMapper wxShopsMapper, WxShopsListService wxShopsListService) {
		this.wxShopsMapper = wxShopsMapper;
		this.wxShopsListService = wxShopsListService;
	}

	public Map<String, Object> getWxShopsList(
			long companyId,
			int page,
			long distributorId,
			boolean distanceBranch,
			String latRaw,
			String lngRaw) {
		long nowSec = Instant.now().getEpochSecond();
		int p = page < 1 ? 1 : page;
		int ps = 500;

		LambdaQueryWrapper<WxShops> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(WxShops::getCompanyId, companyId)
				.gt(WxShops::getExpiredAt, nowSec)
				.eq(WxShops::getIsOpen, Boolean.TRUE)
				.eq(WxShops::getDistributorId, distributorId)
				.orderByDesc(WxShops::getWxShopId);

		Page<WxShops> pageReq = new Page<>(p, ps);
		Page<WxShops> resultPage = wxShopsMapper.selectPage(pageReq, wrapper);
		long total = resultPage.getTotal();

		List<Map<String, Object>> list = new ArrayList<>();
		for (WxShops e : resultPage.getRecords()) {
			list.add(wxShopsListService.toNormalizedListRowForReuse(e, nowSec));
		}

		if (distanceBranch) {
			for (Map<String, Object> row : list) {
				String shopLat = stringifyCoord(row.get("lat"));
				String shopLng = stringifyCoord(row.get("lng"));
				double dMeters = planarDistanceMeters(latRaw, lngRaw, shopLat, shopLng);
				long distance = Math.round(dMeters);
				row.put("distance", distance);
				if (distance > 1000L) {
					double showKm = BigDecimal.valueOf(distance)
							.divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
							.setScale(1, RoundingMode.HALF_UP)
							.doubleValue();
					row.put("distance_show", showKm);
					row.put("distance_unit", "km");
				} else {
					row.put("distance_show", distance);
					row.put("distance_unit", "m");
				}
			}
			list.sort(Comparator.comparingLong(m -> ((Number) m.get("distance")).longValue()));
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", list);
		return data;
	}

	private static String stringifyCoord(Object v) {
		if (v == null) {
			return null;
		}
		return v.toString();
	}

	private static double planarDistanceMeters(
			String userLatRaw, String userLngRaw, String shopLatRaw, String shopLngRaw) {
		String uLatS = userLatRaw == null ? "" : userLatRaw.trim();
		String uLngS = userLngRaw == null ? "" : userLngRaw.trim();
		double uLat;
		double uLng;
		try {
			if (uLatS.isEmpty() || uLngS.isEmpty()) {
				return 0.0;
			}
			uLat = Double.parseDouble(uLatS);
			uLng = Double.parseDouble(uLngS);
			if (!Double.isFinite(uLat) || !Double.isFinite(uLng)) {
				return 0.0;
			}
		} catch (NumberFormatException e) {
			return 0.0;
		}
		if (uLat == 0.0d || uLng == 0.0d) {
			return 0.0;
		}
		double sLat = parseShopDegreesOrZero(shopLatRaw);
		double sLng = parseShopDegreesOrZero(shopLngRaw);

		double lat1Deg = uLat;
		double lng1Deg = uLng;
		double lat2Deg = sLat;
		double lng2Deg = sLng;
		double dxDeg = lng1Deg - lng2Deg;
		double dyDeg = lat1Deg - lat2Deg;
		double bDeg = (lat1Deg + lat2Deg) / 2.0;
		double lx = Math.toRadians(dxDeg) * 6367000.0 * Math.cos(Math.toRadians(bDeg));
		double ly = 6367000.0 * Math.toRadians(dyDeg);
		return Math.sqrt(lx * lx + ly * ly);
	}

	private static double parseShopDegreesOrZero(String raw) {
		if (raw == null) {
			return 0.0;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0.0;
		}
		try {
			double v = Double.parseDouble(t);
			return Double.isFinite(v) ? v : 0.0;
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
}
