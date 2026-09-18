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

import cn.shopex.ecshopx.common.port.wdterp.dto.WdtInventoryWaitSyncPage;
import java.util.Map;

public interface WdtErpInventoryPort {

	WdtInventoryWaitSyncPage fetchWaitSyncPage(
			long companyId, int position, String sid, String appKey, String appSecret);

	/**
	 * 库存明细；解析失败或业务上无有效库存时由实现返回空 Map，由业务层判空。
	 */
	Map<String, Object> queryStore(
			long companyId, String recId, String sid, String appKey, String appSecret);

	void acknowledgeSuccess(
			long companyId, String recId, Map<String, Object> stockInfo, String sid, String appKey, String appSecret);

	void acknowledgeFail(
			long companyId, String recId, Map<String, Object> stockInfo, String sid, String appKey, String appSecret);
}
