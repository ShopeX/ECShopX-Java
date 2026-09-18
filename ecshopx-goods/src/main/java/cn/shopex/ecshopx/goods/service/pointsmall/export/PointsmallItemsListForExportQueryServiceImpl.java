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

package cn.shopex.ecshopx.goods.service.pointsmall.export;

import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PointsmallItemsListForExportQueryServiceImpl implements PointsmallItemsListForExportQueryService {

	private final PointsmallItemsMapper pointsmallItemsMapper;

	public PointsmallItemsListForExportQueryServiceImpl(PointsmallItemsMapper pointsmallItemsMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	@Override
	public List<Long> listDefaultItemIdsForExport(Map<String, Object> params) {
		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		PointsmallItemsExportQuerySupport.applyParams(w, params);
		w.select(PointsmallItems::getDefaultItemId);
		w.orderByDesc(PointsmallItems::getItemId);
		List<PointsmallItems> rows = pointsmallItemsMapper.selectList(w);
		return rows.stream().map(PointsmallItems::getDefaultItemId).filter(id -> id != null && id > 0).distinct()
				.collect(Collectors.toList());
	}
}
