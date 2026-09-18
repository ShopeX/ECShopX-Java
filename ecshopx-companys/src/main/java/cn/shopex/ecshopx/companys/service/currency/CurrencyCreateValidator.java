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
import java.util.List;

public final class CurrencyCreateValidator {

	public static final List<String> ALLOWED_CURRENCY_CODES = CurrencyPresetCatalog.allowedCurrencyCodes();

	private CurrencyCreateValidator() {
	}

	public static void validateCurrencyCode(String currency) {
		if (currency == null || currency.isEmpty()) {
			throw new BadRequestException("货币名称必填");
		}
		if (!ALLOWED_CURRENCY_CODES.contains(currency)) {
			throw new BadRequestException("不支持的货币类型");
		}
	}

	public static void validateNotDuplicate(CurrencyExchangeRate existing) {
		if (existing != null) {
			throw new ResourceException("该币种已存在");
		}
	}
}
