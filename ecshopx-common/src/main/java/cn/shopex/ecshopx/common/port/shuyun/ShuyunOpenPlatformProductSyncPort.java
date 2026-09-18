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

package cn.shopex.ecshopx.common.port.shuyun;

/** 数云开放平台商品同步出站（D3）。goods/distribution 注入；默认 NoOp。 */
public interface ShuyunOpenPlatformProductSyncPort {

	/** auth 允许时按店铺 + SPU（default_item_id）合并派发后同步。 */
	void dispatchIfAuthAllows(long companyId, long distributorId, long defaultItemId);
}
