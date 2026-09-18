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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import java.util.Map;

/**
 * Outbound port for pushing trade-refund-finish order snapshots to the Damo CRM online-store order worker.
 * {@link #syncAfter} and {@link #syncForwardAfter} differ by the {@code orderStatus} sent in the request payload.
 */
public interface DmCrmTradeRefundFinishOrderSyncPort {

	void syncAfter(long companyId, String ruidOrderId, Map<String, Object> requestBody);

	void syncForwardAfter(long companyId, String ruidOrderId, Map<String, Object> requestBody);
}
