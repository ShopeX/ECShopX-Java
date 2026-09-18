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

package cn.shopex.ecshopx.crossborder.service;

import cn.shopex.ecshopx.crossborder.domain.OriginCountry;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OriginCountryListService {

	private final OriginCountryMapper originCountryMapper;

	public OriginCountryListService(OriginCountryMapper originCountryMapper) {
		this.originCountryMapper = originCountryMapper;
	}

	public Map<String, Object> list(long companyId, int page, int pageSize, String keywords) {
		LambdaQueryWrapper<OriginCountry> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(OriginCountry::getCompanyId, companyId);
		wrapper.eq(OriginCountry::getState, 1);
		if (StringUtils.hasText(keywords)) {
			wrapper.like(OriginCountry::getOrigincountryName, "%" + keywords + "%");
		}
		wrapper.orderByDesc(OriginCountry::getCreated);
		Page<OriginCountry> mpPage = new Page<>(page, pageSize);
		Page<OriginCountry> pageResult = originCountryMapper.selectPage(mpPage, wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		for (OriginCountry oc : pageResult.getRecords()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("origincountry_id", oc.getOrigincountryId());
			row.put("origincountry_name", oc.getOrigincountryName());
			row.put("origincountry_img_url", oc.getOrigincountryImgUrl());
			row.put("created", oc.getCreated());
			row.put("updated", oc.getUpdated());
			list.add(row);
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", (int) pageResult.getTotal());
		return data;
	}
}
