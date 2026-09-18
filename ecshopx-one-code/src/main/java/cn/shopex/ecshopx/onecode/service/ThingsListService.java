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

package cn.shopex.ecshopx.onecode.service;

import cn.shopex.ecshopx.onecode.domain.Things;
import cn.shopex.ecshopx.onecode.mapper.ThingsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ThingsListService {

	private final ThingsMapper thingsMapper;

	public ThingsListService(ThingsMapper thingsMapper) {
		this.thingsMapper = thingsMapper;
	}

	public Map<String, Object> list(long companyId, String thingNameOrNull, int page, int pageSize) {
		page = page < 1 ? 1 : page;
		pageSize = pageSize > 100 ? 100 : pageSize;
		pageSize = pageSize <= 0 ? 10 : pageSize;

		LambdaQueryWrapper<Things> w = new LambdaQueryWrapper<>();
		w.eq(Things::getCompanyId, companyId);
		if (StringUtils.hasText(thingNameOrNull)) {
			w.eq(Things::getThingName, thingNameOrNull.trim());
		}
		w.orderByDesc(Things::getCreated);

		Page<Things> mpPage = new Page<>(page, pageSize);
		Page<Things> result = thingsMapper.selectPage(mpPage, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Things row : result.getRecords()) {
			list.add(ThingsRowMapSupport.toThingRowMap(row));
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", (int) result.getTotal());
		return data;
	}
}
