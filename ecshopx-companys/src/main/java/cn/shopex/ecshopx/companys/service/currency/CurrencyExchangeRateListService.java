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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CurrencyExchangeRateListService {

	private static final int DEFAULT_PAGE_SIZE = 100;
	private static final int DEFAULT_PAGE_INDEX = 1;

	private final CurrencyExchangeRateMapper currencyExchangeRateMapper;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public CurrencyExchangeRateListService(
			CurrencyExchangeRateMapper currencyExchangeRateMapper,
			CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.currencyExchangeRateMapper = currencyExchangeRateMapper;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	public Map<String, Object> getDataList(
			long companyId, String currencyQuery, String titleQuery, String isDefaultQuery) {
		LambdaQueryWrapper<CurrencyExchangeRate> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(CurrencyExchangeRate::getCompanyId, companyId);

		if (currencyQuery != null) {
			String c = currencyQuery.trim();
			if (!c.isEmpty()) {
				wrapper.eq(CurrencyExchangeRate::getCurrency, c);
			}
		}

		if (titleQuery != null) {
			String t = titleQuery.trim();
			if (!t.isEmpty()) {
				wrapper.like(CurrencyExchangeRate::getTitle, t);
			}
		}

		Boolean isDefaultEq = null;
		if (isDefaultQuery != null) {
			String t = isDefaultQuery.trim();
			if (!t.isEmpty() && !"0".equals(t)) {
				if ("1".equals(t) || "true".equalsIgnoreCase(t)) {
					isDefaultEq = Boolean.TRUE;
				} else if ("false".equalsIgnoreCase(t)) {
					isDefaultEq = Boolean.FALSE;
				}
			}
		}
		if (isDefaultEq != null) {
			wrapper.eq(CurrencyExchangeRate::getIsDefault, isDefaultEq);
		}

		wrapper.orderByAsc(CurrencyExchangeRate::getCurrency);

		Page<CurrencyExchangeRate> page = new Page<>(DEFAULT_PAGE_INDEX, DEFAULT_PAGE_SIZE);
		currencyExchangeRateMapper.selectPage(page, wrapper);
		long totalCount = page.getTotal();
		List<CurrencyExchangeRate> records = page.getRecords();

		List<Map<String, Object>> rowMaps;
		if (records.isEmpty()) {
			rowMaps = Collections.emptyList();
		} else {
			rowMaps = new ArrayList<>(records.size());
			for (CurrencyExchangeRate entity : records) {
				Map<String, Object> map = companyDefaultCurrencyService.toCurResponseMap(entity);
				if (!map.containsKey("title")) {
					map.put("title", null);
				}
				rowMaps.add(map);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", rowMaps);
		return data;
	}
}
