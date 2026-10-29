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

package cn.shopex.ecshopx.common.operatorcart;

import java.util.Map;

/** 店务立即购买（云仓分桶），由 goods 实现。 */
public interface OperatorCartFastBuyFacade {

	Map<String, Object> addFastBuyCartData(
			long companyId, long operatorId, long distributorId, long itemId, long num, boolean accumulate);

	Map<String, Object> getFastBuyCartDataList(
			long companyId, long operatorId, long distributorId, long targetUserId, boolean isSubmit);

	void revalidateFastBuyPlatformStock(long companyId, long operatorId, long distributorId);

	void clearFastBuyCart(long companyId, long operatorId, long distributorId);
}
