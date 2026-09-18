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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyExchangeRateSetDefaultService {

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;

	public CurrencyExchangeRateSetDefaultService(CurrencyExchangeRateMapper currencyExchangeRateMapper) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void setDefaultCurrency(long companyId, String id) {
		Long pk = tryParseLongOrNull(id);
		LambdaQueryWrapper<CurrencyExchangeRate> wrapper = new LambdaQueryWrapper<>();
		if (pk != null) {
			wrapper.eq(CurrencyExchangeRate::getId, pk);
		} else {
			wrapper.apply("1=0");
		}
		List<CurrencyExchangeRate> rows = currencyExchangeRateMapper.selectList(wrapper);
		if (rows.isEmpty()) {
			throw new ResourceException("货币信息有误");
		}
		CurrencyExchangeRate target = rows.get(0);

		currencyExchangeRateMapper.update(
				null,
				new LambdaUpdateWrapper<CurrencyExchangeRate>()
						.eq(CurrencyExchangeRate::getCompanyId, companyId)
						.set(CurrencyExchangeRate::getIsDefault, false));

		target.setIsDefault(true);
		currencyExchangeRateMapper.updateById(target);
	}

	private static Long tryParseLongOrNull(String pathId) {
		try {
			return Long.parseLong(pathId);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
