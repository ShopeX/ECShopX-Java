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

package cn.shopex.ecshopx.merchant.service;

import org.springframework.util.StringUtils;

/** Filter for tenant-scoped merchant list queries (before pagination). */
public final class MerchantListFilter {

	private final String merchantNameContainsOrNull;
	private final String legalNameEncryptedOrNull;
	private final String legalMobileEncryptedOrNull;
	private final Integer createdGteOrNull;
	private final Integer createdLteOrNull;

	public MerchantListFilter(
			String merchantNameContainsOrNull,
			String legalNameEncryptedOrNull,
			String legalMobileEncryptedOrNull,
			Integer createdGteOrNull,
			Integer createdLteOrNull) {
		this.merchantNameContainsOrNull = blankToNull(merchantNameContainsOrNull);
		this.legalNameEncryptedOrNull = blankToNull(legalNameEncryptedOrNull);
		this.legalMobileEncryptedOrNull = blankToNull(legalMobileEncryptedOrNull);
		this.createdGteOrNull = createdGteOrNull;
		this.createdLteOrNull = createdLteOrNull;
	}

	private static String blankToNull(String s) {
		return StringUtils.hasText(s) ? s.trim() : null;
	}

	public String getMerchantNameContainsOrNull() {
		return merchantNameContainsOrNull;
	}

	public String getLegalNameEncryptedOrNull() {
		return legalNameEncryptedOrNull;
	}

	public String getLegalMobileEncryptedOrNull() {
		return legalMobileEncryptedOrNull;
	}

	public Integer getCreatedGteOrNull() {
		return createdGteOrNull;
	}

	public Integer getCreatedLteOrNull() {
		return createdLteOrNull;
	}
}
