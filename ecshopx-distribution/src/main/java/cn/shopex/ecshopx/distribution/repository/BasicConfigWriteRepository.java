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

package cn.shopex.ecshopx.distribution.repository;

import cn.shopex.ecshopx.distribution.domain.BasicConfig;
import cn.shopex.ecshopx.distribution.mapper.BasicConfigMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class BasicConfigWriteRepository {

	private final BasicConfigMapper basicConfigMapper;

	public BasicConfigWriteRepository(BasicConfigMapper basicConfigMapper) {
		this.basicConfigMapper = basicConfigMapper;
	}

	public BasicConfig getInfoByCompanyId(Long companyId) {
		return basicConfigMapper.selectById(companyId);
	}

	public int updateOneByCompanyId(Long companyId, BasicConfig patch, boolean applyLimitRebate) {
		long now = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<BasicConfig> uw = new LambdaUpdateWrapper<>();
		uw.eq(BasicConfig::getCompanyId, companyId);
		uw.set(BasicConfig::getIsBuy, patch.getIsBuy());
		uw.set(BasicConfig::getLimitTime, patch.getLimitTime());
		uw.set(BasicConfig::getReturnName, patch.getReturnName());
		uw.set(BasicConfig::getReturnAddress, patch.getReturnAddress());
		uw.set(BasicConfig::getReturnPhone, patch.getReturnPhone());
		uw.set(BasicConfig::getIsIncomeTax, patch.getIsIncomeTax());
		uw.set(BasicConfig::getUpdated, now);
		if (applyLimitRebate) {
			uw.set(BasicConfig::getLimitRebate, patch.getLimitRebate());
		}
		return basicConfigMapper.update(null, uw);
	}

	public void create(BasicConfig entity) {
		long now = System.currentTimeMillis() / 1000L;
		if (entity.getCreated() == null) {
			entity.setCreated(now);
		}
		if (entity.getUpdated() == null) {
			entity.setUpdated(now);
		}
		basicConfigMapper.insert(entity);
	}
}
