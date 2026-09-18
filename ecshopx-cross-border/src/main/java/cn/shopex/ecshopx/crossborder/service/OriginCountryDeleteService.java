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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.crossborder.domain.OriginCountry;
import cn.shopex.ecshopx.crossborder.mapper.ItemsOriginCountryRefCountMapper;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class OriginCountryDeleteService {

	private final ItemsOriginCountryRefCountMapper itemsOriginCountryRefCountMapper;
	private final OriginCountryMapper originCountryMapper;

	public OriginCountryDeleteService(
			ItemsOriginCountryRefCountMapper itemsOriginCountryRefCountMapper,
			OriginCountryMapper originCountryMapper) {
		this.itemsOriginCountryRefCountMapper = itemsOriginCountryRefCountMapper;
		this.originCountryMapper = originCountryMapper;
	}

	public void softDelete(long companyId, long origincountryId) {
		long c = itemsOriginCountryRefCountMapper.countByCompanyAndOriginCountry(companyId, origincountryId);
		if (c > 0) {
			throw new ResourceException("有商品正在使用该国家，不可删除");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OriginCountry> wrapper = new LambdaUpdateWrapper<OriginCountry>()
				.eq(OriginCountry::getOrigincountryId, origincountryId)
				.eq(OriginCountry::getCompanyId, companyId)
				.eq(OriginCountry::getState, 1)
				.set(OriginCountry::getState, -1)
				.set(OriginCountry::getUpdated, now);
		int rows = originCountryMapper.update(null, wrapper);
		if (rows <= 0) {
			throw new ResourceException("操作失败");
		}
	}
}
