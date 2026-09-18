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
import java.util.Map;

/**
 * 异步商品码导出的不可变上下文：构造时对 filter 参数做浅表快照。
 */
public final class ItemsCodeExportContext {

	private final long companyId;
	private final long operatorId;
	private final String operatorType;
	private final Long merchantId;
	private final Map<String, Object> filterParams;

	public ItemsCodeExportContext(long companyId, long operatorId, String operatorType, Long merchantId,
			Map<String, Object> source) {
		this.companyId = companyId;
		this.operatorId = operatorId;
		this.operatorType = operatorType != null ? operatorType : "";
		this.merchantId = merchantId;
		this.filterParams = new LinkedHashMap<>(source);
	}

	public long getCompanyId() {
		return companyId;
	}

	public long getOperatorId() {
		return operatorId;
	}

	public String getOperatorType() {
		return operatorType;
	}

	public Long getMerchantId() {
		return merchantId;
	}

	/**
	 * 不可变视图，避免异步外修改。
	 */
	public Map<String, Object> getFilterParams() {
		return Map.copyOf(filterParams);
	}
}
