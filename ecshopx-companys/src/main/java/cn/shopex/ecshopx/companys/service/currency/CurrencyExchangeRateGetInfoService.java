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

import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.mapper.CurrencyExchangeRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class CurrencyExchangeRateGetInfoService {

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;

	public CurrencyExchangeRateGetInfoService(CurrencyExchangeRateMapper currencyExchangeRateMapper) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
	}

	public Object getDataInfo(long companyId, String id) {
		String t = id == null ? "" : id.trim();
		if (t.isEmpty()) {
			return Collections.emptyList();
		}
		long rowId;
		try {
			rowId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}
		if (rowId <= 0L) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<CurrencyExchangeRate> w = new LambdaQueryWrapper<>();
		w.eq(CurrencyExchangeRate::getCompanyId, companyId).eq(CurrencyExchangeRate::getId, rowId);
		CurrencyExchangeRate row = currencyExchangeRateMapper.selectOne(w);
		if (row == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("currency", row.getCurrency());
		m.put("title", row.getTitle());
		m.put("symbol", row.getSymbol());
		m.put("rate", CompanyDefaultCurrencyService.wholeNumberRateForJson(row.getRate()));
		m.put("is_default", row.getIsDefault());
		m.put("use_platform", row.getUsePlatform());
		return m;
	}
}
