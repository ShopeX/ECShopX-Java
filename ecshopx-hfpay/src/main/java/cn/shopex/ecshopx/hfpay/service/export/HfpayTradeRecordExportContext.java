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

package cn.shopex.ecshopx.hfpay.service.export;

/**
 * 汇付分账交易 CSV 导出任务上下文（HTTP 层组装后交由异步执行器处理）。
 */
public final class HfpayTradeRecordExportContext {

	private final long companyId;
	private final long operatorId;
	private final long supplierId;
	private final String startDateTime;
	private final String endDateTime;
	private final Integer distributorId;

	public HfpayTradeRecordExportContext(
			long companyId,
			long operatorId,
			long supplierId,
			String startDateTime,
			String endDateTime,
			Integer distributorId) {
		this.companyId = companyId;
		this.operatorId = operatorId;
		this.supplierId = supplierId;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.distributorId = distributorId;
	}

	public long getCompanyId() {
		return companyId;
	}

	public long getOperatorId() {
		return operatorId;
	}

	public long getSupplierId() {
		return supplierId;
	}

	public String getStartDateTime() {
		return startDateTime;
	}

	public String getEndDateTime() {
		return endDateTime;
	}

	public Integer getDistributorId() {
		return distributorId;
	}
}
