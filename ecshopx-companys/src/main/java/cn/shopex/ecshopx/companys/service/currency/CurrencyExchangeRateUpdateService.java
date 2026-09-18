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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CurrencyExchangeRateUpdateService {

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public CurrencyExchangeRateUpdateService(
			CurrencyExchangeRateMapper currencyExchangeRateMapper,
			CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	public List<Map<String, Object>> updateData(long companyId, String id, Map<String, Object> merged) {
		if (merged == null) {
			merged = Map.of();
		}

		Object currencyRaw = merged.get("currency");
		if (currencyRaw == null || stringValue(currencyRaw).trim().isEmpty()) {
			throw new BadRequestException("货币名称必填");
		}

		Object symbolRaw = merged.get("symbol");
		if (symbolRaw == null || stringValue(symbolRaw).trim().isEmpty()) {
			throw new BadRequestException("货币符号必填");
		}

		Object rateRaw = merged.get("rate");
		if (rateRaw == null || stringValue(rateRaw).trim().isEmpty()) {
			throw new BadRequestException("货币汇率必填");
		}
		double rate = parseRateToDouble(rateRaw);

		Long pk = tryParseLongOrNull(id);
		LambdaQueryWrapper<CurrencyExchangeRate> w =
				new LambdaQueryWrapper<CurrencyExchangeRate>().eq(CurrencyExchangeRate::getCompanyId, companyId);
		if (pk != null) {
			w.eq(CurrencyExchangeRate::getId, pk);
		} else {
			w.apply("1=0");
		}
		List<CurrencyExchangeRate> rows = currencyExchangeRateMapper.selectList(w);
		if (rows.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (CurrencyExchangeRate entity : rows) {
			entity.setCurrency(stringValue(merged.get("currency")).trim());
			entity.setSymbol(stringValue(merged.get("symbol")).trim());
			entity.setRate(rate);

			if (merged.containsKey("title")
					&& StringUtils.hasText(stringValue(merged.get("title")).trim())) {
				entity.setTitle(stringValue(merged.get("title")).trim());
			}

			if (merged.containsKey("use_platform")
					&& StringUtils.hasText(stringValue(merged.get("use_platform")).trim())) {
				entity.setUsePlatform(stringValue(merged.get("use_platform")).trim());
			}

			if (merged.containsKey("is_default")) {
				entity.setIsDefault(parseIsDefaultFlag(merged.get("is_default")));
			}

			currencyExchangeRateMapper.updateById(entity);

			Map<String, Object> row = companyDefaultCurrencyService.toCurResponseMap(entity);
			if (!row.containsKey("title")) {
				row.put("title", null);
			}
			result.add(row);
		}
		return result;
	}

	private static Long tryParseLongOrNull(String pathId) {
		try {
			return Long.parseLong(pathId);
		} catch (NumberFormatException e) {
			return null;
		}
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
