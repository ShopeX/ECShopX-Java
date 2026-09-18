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

package cn.shopex.ecshopx.goods.service.export;

import java.util.LinkedHashMap;

/**
 * 异步商品数据导出任务上下文。
 */
public record ItemsDataExportContext(
		long companyId,
		long operatorId,
		String operatorType,
		Long merchantId,
		String exportType,
		String itemSource,
		LinkedHashMap<String, Object> filterParams) {

	public ItemsDataExportContext {
		filterParams = filterParams != null ? new LinkedHashMap<>(filterParams) : new LinkedHashMap<>();
	}
}
