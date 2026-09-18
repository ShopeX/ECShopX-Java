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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.mapper.CurrencyExchangeRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CurrencyExchangeRateCreateService {

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public CurrencyExchangeRateCreateService(
			CurrencyExchangeRateMapper currencyExchangeRateMapper,
			CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	public Map<String, Object> create(Map<String, Object> body, long companyId) {
		if (body == null) {
			body = Map.of();
		}

		Object currencyRaw = body.get("currency");
		if (currencyRaw == null || stringValue(currencyRaw).trim().isEmpty()) {
			throw new BadRequestException("货币名称必填");
		}
		String currency = stringValue(currencyRaw).trim();
		CurrencyCreateValidator.validateCurrencyCode(currency);

		Object symbolRaw = body.get("symbol");
		if (symbolRaw == null || stringValue(symbolRaw).trim().isEmpty()) {
			throw new BadRequestException("货币符号必填");
		}
		String symbol = stringValue(symbolRaw).trim();

		Object rateRaw = body.get("rate");
		if (rateRaw == null || stringValue(rateRaw).trim().isEmpty()) {
			throw new BadRequestException("货币汇率必填");
		}
		double rate = parseRateToDouble(rateRaw);

		CurrencyExchangeRate existing =
				currencyExchangeRateMapper.selectOne(
						new LambdaQueryWrapper<CurrencyExchangeRate>()
								.eq(CurrencyExchangeRate::getCompanyId, companyId)
								.eq(CurrencyExchangeRate::getCurrency, currency)
								.last("LIMIT 1"));
		CurrencyCreateValidator.validateNotDuplicate(existing);

		CurrencyExchangeRate entity = new CurrencyExchangeRate();
		entity.setCompanyId(companyId);
		entity.setCurrency(currency);
		entity.setSymbol(symbol);
		entity.setRate(rate);

		if (body.containsKey("title")) {
			String title = stringValue(body.get("title")).trim();
			if (!title.isEmpty()) {
				entity.setTitle(title);
			}
		}

		if (body.containsKey("use_platform")) {
			String usePlatform = stringValue(body.get("use_platform")).trim();
			if (!usePlatform.isEmpty()) {
				entity.setUsePlatform(usePlatform);
			}
		}

		if (body.containsKey("is_default")) {
			entity.setIsDefault(parseIsDefaultFlag(body.get("is_default")));
		}

		try {
			currencyExchangeRateMapper.insert(entity);
		} catch (DataIntegrityViolationException e) {
			throw new ResourceException("该币种已存在");
		}

		Map<String, Object> data = companyDefaultCurrencyService.toCurResponseMap(entity);
		if (!data.containsKey("title")) {
			data.put("title", null);
		}
		return data;
	}

	private static String stringValue(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s;
		}
		return String.valueOf(o);
	}

	private static double parseRateToDouble(Object raw) {
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			assertRateNumericLegality(d);
			return d;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("货币汇率必填");
			}
			try {
				double d = Double.parseDouble(t);
				assertRateNumericLegality(d);
				return d;
			} catch (NumberFormatException e) {
				throw new BadRequestException("货币汇率格式错误");
			}
		}
		String t = stringValue(raw).trim();
		if (t.isEmpty()) {
			throw new BadRequestException("货币汇率格式错误");
		}
		try {
			double d = Double.parseDouble(t);
			assertRateNumericLegality(d);
			return d;
		} catch (NumberFormatException e) {
			throw new BadRequestException("货币汇率格式错误");
		}
	}

	private static void assertRateNumericLegality(double d) {
		if (Double.isNaN(d)) {
			throw new BadRequestException("货币汇率无效");
		}
		if (Double.isInfinite(d)) {
			throw new BadRequestException("货币汇率超出允许范围");
		}
	}

	private static boolean parseIsDefaultFlag(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof String s) {
			if (s.isEmpty() || s.equalsIgnoreCase("false")) {
				return false;
			}
			return true;
		}
		return true;
	}
}
