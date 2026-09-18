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

import cn.shopex.ecshopx.onecode.domain.Batchs;
import cn.shopex.ecshopx.onecode.mapper.BatchsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BatchsListService {

	private final BatchsMapper batchsMapper;

	public BatchsListService(BatchsMapper batchsMapper) {
		this.batchsMapper = batchsMapper;
	}

	public Map<String, Object> list(long companyId, Long thingId, int page, int pageSize) {
		page = page < 1 ? 1 : page;
		pageSize = pageSize > 100 ? 100 : pageSize;
		pageSize = pageSize <= 0 ? 10 : pageSize;

		LambdaQueryWrapper<Batchs> w = new LambdaQueryWrapper<>();
		w.eq(Batchs::getCompanyId, companyId);
		if (thingId == null) {
			w.isNull(Batchs::getThingId);
		} else {
			w.eq(Batchs::getThingId, thingId);
		}
		w.orderByDesc(Batchs::getCreated);

		Page<Batchs> mpPage = new Page<>(page, pageSize);
		Page<Batchs> result = batchsMapper.selectPage(mpPage, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Batchs row : result.getRecords()) {
			list.add(BatchsRowMapSupport.toBatchRowMap(row));
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", (int) result.getTotal());
		return data;
	}
}
