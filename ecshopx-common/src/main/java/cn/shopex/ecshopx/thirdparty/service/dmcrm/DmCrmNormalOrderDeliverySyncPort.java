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
 * Outbound call when a normal order shipment is recorded, to keep the Damo CRM online-store order view in sync.
 */
public interface DmCrmNormalOrderDeliverySyncPort {

	void syncNormalOrderDelivery(long companyId, long orderId, Map<String, Object> busPayload);
}
