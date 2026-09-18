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

import java.util.Optional;

/**
 * Resolves the main merchant console {@code operators.operator_id} for a company and merchant row,
 * without pulling {@code ecshopx-companys} mappers into {@code ecshopx-merchant} (Maven cycle).
 */
public interface MerchantWxappMainOperatorIdLookupPort {

	Optional<Long> findMainMerchantOperatorId(long companyId, long merchantId);
}
