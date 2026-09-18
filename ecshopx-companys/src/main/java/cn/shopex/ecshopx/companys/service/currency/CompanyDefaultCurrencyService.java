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
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompanyDefaultCurrencyService {

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;

	public CompanyDefaultCurrencyService(CurrencyExchangeRateMapper currencyExchangeRateMapper) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
	}

	public CurrencyExchangeRate getCur(long companyId) {
		CurrencyExchangeRate row = currencyExchangeRateMapper.selectOne(
				new LambdaQueryWrapper<CurrencyExchangeRate>()
						.eq(CurrencyExchangeRate::getCompanyId, companyId)
						.eq(CurrencyExchangeRate::getIsDefault, true)
						.last("LIMIT 1"));
		if (row != null) {
			return row;
		}
		CurrencyExchangeRate insert = new CurrencyExchangeRate();
		insert.setCompanyId(companyId);
		insert.setCurrency("CNY");
		insert.setTitle("中国人民币");
		insert.setSymbol("￥");
		insert.setRate(1.0);
		insert.setIsDefault(true);
		currencyExchangeRateMapper.insert(insert);
		CurrencyExchangeRate again = currencyExchangeRateMapper.selectOne(
				new LambdaQueryWrapper<CurrencyExchangeRate>()
						.eq(CurrencyExchangeRate::getCompanyId, companyId)
						.eq(CurrencyExchangeRate::getIsDefault, true)
						.last("LIMIT 1"));
		if (again == null) {
			throw new ResourceException("默认货币初始化失败");
		}
		return again;
	}

	public Map<String, Object> toCurResponseMap(CurrencyExchangeRate r) {
		if (r == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		putIfNonNull(m, "id", r.getId());
		putIfNonNull(m, "company_id", r.getCompanyId());
		putIfNonNull(m, "currency", r.getCurrency());
		putIfNonNull(m, "title", r.getTitle());
		putIfNonNull(m, "symbol", r.getSymbol());
		putIfNonNull(m, "rate", wholeNumberRateForJson(r.getRate()));
		putIfNonNull(m, "is_default", r.getIsDefault());
		putIfNonNull(m, "use_platform", r.getUsePlatform());
		return m;
	}

	private static void putIfNonNull(Map<String, Object> m, String key, Object val) {
		if (val != null) {
			m.put(key, val);
		}
	}

	/**
	 * Some clients emit {@code cur.rate} as JSON integer when whole; Jackson would emit {@code 1.0} for {@link Double}.
	 */
	public static Number wholeNumberRateForJson(Double rate) {
		if (rate == null) {
			return null;
		}
		double d = rate;
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return rate;
		}
		if (d == (long) d) {
			long lv = (long) d;
			if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
				return (int) lv;
			}
			return lv;
		}
		return rate;
	}
}
