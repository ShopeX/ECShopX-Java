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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxShopsNearestService {

	public static record NearestWxShopsResult(boolean coordinateBranch, Object payload) {

		public List<Map<String, Object>> payloadAsList() {
			if (!coordinateBranch) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> list = (List<Map<String, Object>>) payload;
				return list;
			}
			throw new IllegalStateException();
		}

		public Object payloadAsCoordBody() {
			if (coordinateBranch) {
				return payload;
			}
			throw new IllegalStateException();
		}
	}

	private final WxShopsMapper wxShopsMapper;
	private final WxShopsListService wxShopsListService;

	public WxShopsNearestService(WxShopsMapper wxShopsMapper, WxShopsListService wxShopsListService) {
		this.wxShopsMapper = wxShopsMapper;
		this.wxShopsListService = wxShopsListService;
	}

	public NearestWxShopsResult getNearestWxShops(
			long companyId, boolean coordinateBranch, String userLatRaw, String userLngRaw) {
		long nowSec = Instant.now().getEpochSecond();

		if (!coordinateBranch) {
			LambdaQueryWrapper<WxShops> def = new LambdaQueryWrapper<WxShops>()
					.eq(WxShops::getCompanyId, companyId)
					.gt(WxShops::getExpiredAt, nowSec)
					.eq(WxShops::getIsDefault, true)
					.orderByDesc(WxShops::getWxShopId);
			Page<WxShops> page1 = new Page<>(1, 1);
			Page<WxShops> first = wxShopsMapper.selectPage(page1, def);
			List<WxShops> recs = first.getRecords();
			if (recs.isEmpty()) {
				LambdaQueryWrapper<WxShops> fallback = new LambdaQueryWrapper<WxShops>()
						.eq(WxShops::getCompanyId, companyId)
						.gt(WxShops::getExpiredAt, nowSec)
						.orderByDesc(WxShops::getWxShopId);
				Page<WxShops> page2 = new Page<>(1, 1);
				first = wxShopsMapper.selectPage(page2, fallback);
				recs = first.getRecords();
			}
			List<Map<String, Object>> list = new ArrayList<>();
			for (WxShops e : recs) {
				list.add(wxShopsListService.toNormalizedListRowForReuse(e, nowSec));
			}
			return new NearestWxShopsResult(false, list);
		}

		LambdaQueryWrapper<WxShops> wrapper =
				new LambdaQueryWrapper<WxShops>().eq(WxShops::getCompanyId, companyId).gt(WxShops::getExpiredAt, nowSec);
		wrapper.orderByDesc(WxShops::getWxShopId);
		Page<WxShops> page = new Page<>(1, 500);
		Page<WxShops> resultPage = wxShopsMapper.selectPage(page, wrapper);
		List<WxShops> records = resultPage.getRecords();
		if (records.isEmpty()) {
			return new NearestWxShopsResult(true, Collections.emptyList());
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		List<Long> distances = new ArrayList<>();
		for (WxShops e : records) {
			Map<String, Object> row =
					new LinkedHashMap<>(wxShopsListService.toNormalizedListRowForReuse(e, nowSec));
			double dMeters = planarDistanceMeters(userLatRaw, userLngRaw, e.getLat(), e.getLng());
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
			rows.add(row);
			distances.add(distance);
		}

		int bestIndex = -1;
		long best = Long.MAX_VALUE;
		for (int i = 0; i < distances.size(); i++) {
			long d = distances.get(i);
			if (d < best) {
				best = d;
				bestIndex = i;
			}
		}
		return new NearestWxShopsResult(true, rows.get(bestIndex));
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
