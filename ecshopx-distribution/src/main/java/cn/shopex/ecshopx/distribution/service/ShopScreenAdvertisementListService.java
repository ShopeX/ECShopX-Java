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
public class ShopScreenAdvertisementListService {

	private final AdvertisementMapper advertisementMapper;

	public ShopScreenAdvertisementListService(AdvertisementMapper advertisementMapper) {
		this.advertisementMapper = advertisementMapper;
	}

	public Map<String, Object> getAdvertisement(long companyId, long distributorId, int page, int pageSize) {
		LambdaQueryWrapper<Advertisement> base = new LambdaQueryWrapper<>();
		base.eq(Advertisement::getCompanyId, companyId);
		base.eq(Advertisement::getDistributorId, distributorId);

		long totalLong = advertisementMapper.selectCount(base);
		int totalCount = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);

		if (totalCount == 0) {
			result.put("list", List.of());
		} else {
			LambdaQueryWrapper<Advertisement> q = new LambdaQueryWrapper<>();
			q.eq(Advertisement::getCompanyId, companyId);
			q.eq(Advertisement::getDistributorId, distributorId);
			q.orderByDesc(Advertisement::getSort).orderByDesc(Advertisement::getCreated);

			Page<Advertisement> p = new Page<>(page, pageSize, false);
			List<Advertisement> records = advertisementMapper.selectPage(p, q).getRecords();

			List<Map<String, Object>> list = new ArrayList<>(records.size());
			for (Advertisement row : records) {
				list.add(AdvertisementColumnNamesDataMapper.toColumnNamesData(row));
			}
			result.put("list", list);
		}

		return result;
	}
}
