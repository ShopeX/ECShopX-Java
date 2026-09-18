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
import cn.shopex.ecshopx.crossborder.domain.Taxstrategy;
import cn.shopex.ecshopx.crossborder.mapper.TaxstrategyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class TaxstrategyDeleteService {

	private final TaxstrategyMapper taxstrategyMapper;

	public TaxstrategyDeleteService(TaxstrategyMapper taxstrategyMapper) {
		this.taxstrategyMapper = taxstrategyMapper;
	}

	public void softDelete(long companyId, long taxstrategyId) {
		Taxstrategy row = taxstrategyMapper.selectOne(new LambdaQueryWrapper<Taxstrategy>()
				.eq(Taxstrategy::getId, taxstrategyId)
				.eq(Taxstrategy::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException("操作失败");
		}
		if (row.getState() == null || row.getState() != 1) {
			throw new ResourceException("操作失败");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Taxstrategy> wrapper = new LambdaUpdateWrapper<Taxstrategy>()
				.eq(Taxstrategy::getId, taxstrategyId)
				.eq(Taxstrategy::getCompanyId, companyId)
				.set(Taxstrategy::getState, -1)
				.set(Taxstrategy::getUpdated, now);
		int rows = taxstrategyMapper.update(null, wrapper);
		if (rows <= 0) {
			throw new ResourceException("操作失败");
		}
	}
}
