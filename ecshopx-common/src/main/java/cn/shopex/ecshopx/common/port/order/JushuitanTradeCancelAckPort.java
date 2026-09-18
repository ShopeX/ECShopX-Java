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

package cn.shopex.ecshopx.common.port.order;

import java.util.Map;

public interface JushuitanTradeCancelAckPort {

	/**
	 * Removes the platform link row, then optionally runs the pointsmall agree-cancel domain step when
	 * {@code tradeCancelPayload} is non-null (same entry shape as the trade-cancel dispatch payload).
	 *
	 * @param tradeCancelPayload context from the trade-cancel event; may be {@code null} to only clear the link row
	 */
	void acknowledgeAfterPlatformCancel(long companyId, long orderId, Map<String, Object> tradeCancelPayload);
}
