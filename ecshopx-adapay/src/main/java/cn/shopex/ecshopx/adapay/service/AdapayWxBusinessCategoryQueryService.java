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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayWxBusinessCategory;
import cn.shopex.ecshopx.adapay.mapper.AdapayWxBusinessCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AdapayWxBusinessCategoryQueryService {

	private final AdapayWxBusinessCategoryMapper adapayWxBusinessCategoryMapper;

	public AdapayWxBusinessCategoryQueryService(AdapayWxBusinessCategoryMapper adapayWxBusinessCategoryMapper) {
		this.adapayWxBusinessCategoryMapper = adapayWxBusinessCategoryMapper;
	}

	public Map<String, Object> getWxBusinessCatList(String feeType, String merchantTypeName) {
		LambdaQueryWrapper<AdapayWxBusinessCategory> wrapper = new LambdaQueryWrapper<>();
		if (nonEmptyString(feeType)) {
			wrapper.eq(AdapayWxBusinessCategory::getFeeType, feeType);
		}
		if (nonEmptyString(merchantTypeName)) {
			wrapper.like(AdapayWxBusinessCategory::getMerchantTypeName, merchantTypeName);
		}

		long totalCount = adapayWxBusinessCategoryMapper.selectCount(wrapper);

		List<Map<String, Object>> listRows;
		if (totalCount == 0) {
			listRows = Collections.emptyList();
		} else {
			List<AdapayWxBusinessCategory> rawList = adapayWxBusinessCategoryMapper.selectList(wrapper);
			LinkedHashMap<String, AdapayWxBusinessCategory> dedup = new LinkedHashMap<>();
			for (AdapayWxBusinessCategory row : rawList) {
				String key = Objects.toString(row.getMerchantTypeName(), "")
						+ "_" + Objects.toString(row.getBusinessCategoryId(), "");
				dedup.put(key, row);
			}
			listRows = new ArrayList<>(dedup.size());
			for (AdapayWxBusinessCategory row : dedup.values()) {
				Map<String, Object> map = new HashMap<>();
				map.put("id", row.getId());
				map.put("fee_type", row.getFeeType());
				map.put("fee_type_name", row.getFeeTypeName());
				map.put("merchant_type_name", row.getMerchantTypeName());
				map.put("business_category_id", row.getBusinessCategoryId());
				listRows.add(map);
			}
		}

		Map<String, Object> data = new HashMap<>();
		data.put("total_count", totalCount);
		data.put("list", listRows);
		return data;
	}

	private static boolean nonEmptyString(String v) {
		return v != null && !v.isEmpty();
	}
}
