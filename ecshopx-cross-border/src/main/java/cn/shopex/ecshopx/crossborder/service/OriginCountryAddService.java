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
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

@Service
public class OriginCountryAddService {

	private final OriginCountryMapper originCountryMapper;

	public OriginCountryAddService(OriginCountryMapper originCountryMapper) {
		this.originCountryMapper = originCountryMapper;
	}

	public long add(long companyId, String origincountryName, String origincountryImgUrl) {
		long dupCount = originCountryMapper.selectCount(
				Wrappers.lambdaQuery(OriginCountry.class)
						.eq(OriginCountry::getCompanyId, companyId)
						.eq(OriginCountry::getOrigincountryName, origincountryName)
						.eq(OriginCountry::getState, 1));
		if (dupCount > 0) {
			throw new ResourceException("该国家已经存在");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		OriginCountry entity = new OriginCountry();
		entity.setCompanyId(companyId);
		entity.setState(1);
		entity.setOrigincountryName(origincountryName);
		entity.setOrigincountryImgUrl(origincountryImgUrl);
		entity.setCreated(now);
		entity.setUpdated(now);
		originCountryMapper.insert(entity);
		Long id = entity.getOrigincountryId();
		if (id == null || id <= 0) {
			throw new ResourceException("操作失败");
		}
		return id.longValue();
	}
}
