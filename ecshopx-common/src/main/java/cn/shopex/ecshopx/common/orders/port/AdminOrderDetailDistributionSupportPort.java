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

package cn.shopex.ecshopx.common.orders.port;

import java.util.Map;

/**
 * Distribution-side reads for admin order detail payloads. Implemented in {@code ecshopx-distribution} to avoid a
 * Maven cycle ({@code ecshopx-distribution} already depends on {@code ecshopx-orders}).
 */
public interface AdminOrderDetailDistributionSupportPort {

	Map<String, Object> getDistributorInfoSimple(long companyId, String distributorIdStr);

	Map<String, Object> getDistributorSelfSimpleInfo(long companyId);

	/** PHP {@code DistributorService::getInfo} — includes {@code formatStoreInfo} / {@code selfDeliveryRule}. */
	Map<String, Object> getDistributorInfoFormatted(long companyId, long distributorId);

	Map<String, Object> readOrderValidityPlatformSetting(long companyId);

	Map<String, Object> resolveWxappListDistributorFilter(long companyId, String mobileFromAuth);
}
