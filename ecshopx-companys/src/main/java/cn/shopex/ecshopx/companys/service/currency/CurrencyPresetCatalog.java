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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CurrencyPresetCatalog {

	public record CurrencyPreset(String currency, String title, String label, String symbol) {}

	private static final List<CurrencyPreset> PRESETS =
			List.of(
					new CurrencyPreset("CNY", "中国人民币", "人民币", "￥"),
					new CurrencyPreset("HKD", "香港港币", "港币", "HK$"),
					new CurrencyPreset("USD", "美国美元", "美元", "$"),
					new CurrencyPreset("TWD", "台湾台币", "台币", "NT$"),
					new CurrencyPreset("JPY", "日本日元", "日元", "¥"),
					new CurrencyPreset("RUB", "俄罗斯卢布", "卢布", "₽"));

	private CurrencyPresetCatalog() {}

	public static List<String> allowedCurrencyCodes() {
		return PRESETS.stream().map(CurrencyPreset::currency).toList();
	}

	public static Optional<CurrencyPreset> findByCode(String currency) {
		if (currency == null || currency.isBlank()) {
			return Optional.empty();
		}
		String code = currency.trim();
		return PRESETS.stream().filter(p -> p.currency().equals(code)).findFirst();
	}

	public static List<Map<String, Object>> toOptionList() {
		return PRESETS.stream().map(CurrencyPresetCatalog::toOptionMap).toList();
	}

	private static Map<String, Object> toOptionMap(CurrencyPreset preset) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("currency", preset.currency());
		row.put("title", preset.title());
		row.put("label", preset.label());
		row.put("symbol", preset.symbol());
		return row;
	}
}
