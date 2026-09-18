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

package cn.shopex.ecshopx.companys.service.currency;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.mapper.CurrencyExchangeRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyExchangeRateDeleteService {

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;

	public CurrencyExchangeRateDeleteService(CurrencyExchangeRateMapper currencyExchangeRateMapper) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteData(long companyId, String idPath) {
		String p = idPath == null ? "" : idPath.trim();
		if (p.isEmpty()) {
			throw new ResourceException("删除的数据不存在");
		}
		long rowId;
		try {
			rowId = Long.parseLong(p);
		} catch (NumberFormatException e) {
			throw new ResourceException("删除的数据不存在");
		}
		if (rowId <= 0L) {
			throw new ResourceException("删除的数据不存在");
		}
		LambdaQueryWrapper<CurrencyExchangeRate> q =
				new LambdaQueryWrapper<CurrencyExchangeRate>()
						.eq(CurrencyExchangeRate::getCompanyId, companyId)
						.eq(CurrencyExchangeRate::getId, rowId);
		CurrencyExchangeRate row = currencyExchangeRateMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("删除的数据不存在");
		}
		if (Boolean.TRUE.equals(row.getIsDefault())) {
			throw new ResourceException("默认货币，不可删除");
		}
		int removed = currencyExchangeRateMapper.delete(q);
		if (removed == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}
}
