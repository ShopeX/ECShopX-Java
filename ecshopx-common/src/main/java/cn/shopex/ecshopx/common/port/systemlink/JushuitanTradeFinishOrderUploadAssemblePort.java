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

package cn.shopex.ecshopx.common.port.systemlink;

import java.util.List;
import java.util.Map;

/**
 * Builds Jushuitan order-upload biz payloads from a persisted trade row map (snake_case keys).
 */
public interface JushuitanTradeFinishOrderUploadAssemblePort {

	/**
	 * @param jstShopId resolved Jushuitan shop id (company default or distributor {@code jst_shop_id})
	 * @return empty when nothing should be sent (listener early-return / skip)
	 */
	List<Map<String, Object>> assembleOrderUploadPayloads(
			long companyId, Map<String, Object> tradeRowSnakeCase, String jstShopId);
}
