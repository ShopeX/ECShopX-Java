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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.domain.DigiOsStoreMapping;
import cn.shopex.ecshopx.digios.mapper.DigiOsStoreMappingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigiOsStoreMappingService {

	private final DigiOsStoreMappingMapper mappingMapper;

	public DigiOsStoreMappingService(DigiOsStoreMappingMapper mappingMapper) {
		this.mappingMapper = mappingMapper;
	}

	public Long findDigiosStoreId(long companyId, long distributorId) {
		DigiOsStoreMapping row = mappingMapper.selectOne(
				new LambdaQueryWrapper<DigiOsStoreMapping>()
						.eq(DigiOsStoreMapping::getCompanyId, companyId)
						.eq(DigiOsStoreMapping::getDistributorId, distributorId)
						.last("LIMIT 1"));
		if (row == null || row.getDigiosStoreId() == null || row.getDigiosStoreId() <= 0) {
			return null;
		}
		return row.getDigiosStoreId();
	}

	public void saveDigiosStoreId(long companyId, long distributorId, String shopCode, long digiosStoreId) {
		int now = (int) Instant.now().getEpochSecond();
		DigiOsStoreMapping existing = mappingMapper.selectOne(
				new LambdaQueryWrapper<DigiOsStoreMapping>()
						.eq(DigiOsStoreMapping::getCompanyId, companyId)
						.eq(DigiOsStoreMapping::getDistributorId, distributorId)
						.last("LIMIT 1"));
		if (existing != null) {
			existing.setShopCode(StringUtils.hasText(shopCode) ? shopCode : existing.getShopCode());
			existing.setDigiosStoreId(digiosStoreId);
			existing.setUpdated(now);
			mappingMapper.updateById(existing);
			return;
		}
		DigiOsStoreMapping row = new DigiOsStoreMapping();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setShopCode(shopCode == null ? "" : shopCode);
		row.setDigiosStoreId(digiosStoreId);
		row.setCreated(now);
		row.setUpdated(now);
		mappingMapper.insert(row);
	}
}
