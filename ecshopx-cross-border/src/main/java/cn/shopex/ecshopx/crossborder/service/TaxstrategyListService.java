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

import cn.shopex.ecshopx.crossborder.domain.Taxstrategy;
import cn.shopex.ecshopx.crossborder.mapper.TaxstrategyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxstrategyListService {

	private final TaxstrategyMapper taxstrategyMapper;

	public TaxstrategyListService(TaxstrategyMapper taxstrategyMapper) {
		this.taxstrategyMapper = taxstrategyMapper;
	}

	public Map<String, Object> list(long companyId, int page, int pageSize, String keywords) {
		LambdaQueryWrapper<Taxstrategy> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Taxstrategy::getCompanyId, companyId);
		wrapper.eq(Taxstrategy::getState, 1);
		if (StringUtils.hasText(keywords)) {
			wrapper.like(Taxstrategy::getTaxstrategyName, "%" + keywords + "%");
		}
		wrapper.orderByDesc(Taxstrategy::getCreated);
		Page<Taxstrategy> mpPage = new Page<>(page, pageSize);
		Page<Taxstrategy> pageResult = taxstrategyMapper.selectPage(mpPage, wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Taxstrategy ts : pageResult.getRecords()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", ts.getId());
			row.put("taxstrategy_name", ts.getTaxstrategyName());
			row.put("created", ts.getCreated());
			row.put("updated", ts.getUpdated());
			list.add(row);
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", (int) pageResult.getTotal());
		return data;
	}
}
