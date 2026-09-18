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

package cn.shopex.ecshopx.merchant.port;

/** Creates the merchant main operator account; implemented in ecshopx-companys. */
public interface MerchantMainOperatorProvisioner {

	/**
	 * @return plaintext 6-digit numeric password for the new operator
	 */
	String createMainMerchantOperator(long companyId, String mobilePlain, String loginNamePlain, long merchantId);

	void createMainMerchantOperatorWithoutPassword(long companyId, String mobilePlain, String loginNamePlain, long merchantId);
}
