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

import cn.shopex.ecshopx.distribution.domain.Advertisement;
import cn.shopex.ecshopx.distribution.mapper.AdvertisementMapper;
import cn.shopex.ecshopx.distribution.support.AdvertisementColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ShopScreenAdvertisementStartAdsService {

	private final AdvertisementMapper advertisementMapper;

	public ShopScreenAdvertisementStartAdsService(AdvertisementMapper advertisementMapper) {
		this.advertisementMapper = advertisementMapper;
	}

	public Map<String, Object> getAdvertisements(long companyId, long distributorId) {
		long queryDistributorId = distributorId;

		LambdaQueryWrapper<Advertisement> c1 = new LambdaQueryWrapper<>();
		c1.eq(Advertisement::getCompanyId, companyId);
		c1.eq(Advertisement::getDistributorId, queryDistributorId);
		c1.eq(Advertisement::getReleaseStatus, Boolean.TRUE);
		long totalForShop = advertisementMapper.selectCount(c1);

		long effectiveDistributorId = totalForShop == 0L ? 0L : distributorId;

		LambdaQueryWrapper<Advertisement> q = new LambdaQueryWrapper<>();
		q.eq(Advertisement::getCompanyId, companyId);
		q.eq(Advertisement::getDistributorId, effectiveDistributorId);
		q.eq(Advertisement::getReleaseStatus, Boolean.TRUE);
		q.orderByDesc(Advertisement::getSort).orderByDesc(Advertisement::getCreated);

		long dbTotal = advertisementMapper.selectCount(q);
		Page<Advertisement> page = new Page<>(1, 100, false);
		Page<Advertisement> resultPage = advertisementMapper.selectPage(page, q);
		List<Advertisement> records = resultPage.getRecords();

		int count = (int) Math.min(dbTotal, (long) Integer.MAX_VALUE);
		if (count == 0) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0);
			empty.put("list", List.of());
			return empty;
		}

		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (Advertisement row : records) {
			list.add(new LinkedHashMap<>(AdvertisementColumnNamesDataMapper.toColumnNamesData(row)));
		}

		int frontendShowTotal = 3;
		int loopCount = count;
		while (loopCount++ < frontendShowTotal) {
			Map<String, Object> last = list.get(list.size() - 1);
			list.add(copyAdvertisementRowMap(last));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", 3);
		result.put("list", list);

		for (Map<String, Object> rowMap : list) {
			Object url = rowMap.get("media_url");
			Object type = rowMap.get("media_type");
			rowMap.put(
					"media",
					Map.of("url", url != null ? url : "", "type", type != null ? type : ""));
		}

		result.put("thumb_img", list.stream().map(m -> m.get("thumb_img")).toList());
		result.put("media", list.stream().map(m -> m.get("media")).toList());

		return result;
	}

	private static Map<String, Object> copyAdvertisementRowMap(Map<String, Object> source) {
		Map<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : source.entrySet()) {
			copy.put(e.getKey(), e.getValue());
		}
		return copy;
	}
}
