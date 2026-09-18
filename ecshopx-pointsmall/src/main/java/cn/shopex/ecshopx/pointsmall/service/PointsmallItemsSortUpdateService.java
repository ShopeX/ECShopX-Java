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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class PointsmallItemsSortUpdateService {

	private final PointsmallItemsMapper pointsmallItemsMapper;

	public PointsmallItemsSortUpdateService(PointsmallItemsMapper pointsmallItemsMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	public void setItemsSort(long companyId, long itemId, int sort) {
		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<PointsmallItems>()
				.eq(PointsmallItems::getItemId, itemId)
				.last("LIMIT 1");
		PointsmallItems row = pointsmallItemsMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("请确认您的商品信息后再提交。");
		}
		if (row.getCompanyId() == null || row.getCompanyId() != companyId) {
			throw new ResourceException("请确认您的商品信息后再提交。");
		}
		int now = (int) (System.currentTimeMillis() / 1000);
		LambdaUpdateWrapper<PointsmallItems> u = new LambdaUpdateWrapper<PointsmallItems>()
				.eq(PointsmallItems::getItemId, itemId)
				.set(PointsmallItems::getSort, sort)
				.set(PointsmallItems::getUpdated, now);
		pointsmallItemsMapper.update(null, u);
	}
}
