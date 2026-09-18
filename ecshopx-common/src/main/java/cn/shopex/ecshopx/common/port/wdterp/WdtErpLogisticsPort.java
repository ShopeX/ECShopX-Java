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

package cn.shopex.ecshopx.common.port.wdterp;

import java.util.List;
import java.util.Map;

/**
 * 旺店通待同步物流拉取与回写；生产实现由 system-link 侧委托 {@code WdtErpOpenApiClient}。
 */
public interface WdtErpLogisticsPort {

	/**
	 * 拉取单页待同步物流（与 PHP {@code logistics_get_wait_sync} / pageCall 语义一致）。
	 *
	 * @return 物流行列表，元素为字段与旺店通响应一致的 map（含 {@code tid}、{@code sync_id} 等）
	 */
	List<Map<String, Object>> getWaitSyncPage(
			long companyId,
			String shopNo,
			int pageNo,
			String sid,
			String appKey,
			String appSecret);

	/**
	 * 回写物流同步结果（与 PHP {@code logistics_sync_success} / call 语义一致），{@code items} 须含 {@code sync_id} 与
	 * {@code status}（0/2）。
	 */
	void acknowledgeSync(
			long companyId, List<Map<String, Object>> items, String sid, String appKey, String appSecret);
}
