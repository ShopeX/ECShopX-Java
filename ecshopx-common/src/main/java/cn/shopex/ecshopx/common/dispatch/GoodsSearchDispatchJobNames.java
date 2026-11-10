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

package cn.shopex.ecshopx.common.dispatch;

public final class GoodsSearchDispatchJobNames {

	/** 商品 ES 搜索索引全量/按店重建（payload: company_id, 可选 distributor_id）。 */
	public static final String SEARCH_REBUILD_JOB = "job:198:GoodsBundle\\Jobs\\SearchRebuildJob";

	private GoodsSearchDispatchJobNames() {
	}
}
