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

/** 汇付提现记录 CSV 导出任务上下文（HTTP 层组装后交由异步执行器处理）。 */
public final class HfpayWithdrawRecordExportContext {

	private final long companyId;
	private final long operatorId;
	private final String startDateTime;
	private final String endDateTime;
	private final Long distributorId;
	private final String orderId;
	private final boolean cashStatusInProgress;
	private final Integer cashStatusExact;

	public HfpayWithdrawRecordExportContext(
			long companyId,
			long operatorId,
			String startDateTime,
			String endDateTime,
			Long distributorId,
			String orderId,
			boolean cashStatusInProgress,
			Integer cashStatusExact) {
		this.companyId = companyId;
		this.operatorId = operatorId;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.distributorId = distributorId;
		this.orderId = orderId;
		this.cashStatusInProgress = cashStatusInProgress;
		this.cashStatusExact = cashStatusExact;
	}

	public long getCompanyId() {
		return companyId;
	}

	public long getOperatorId() {
		return operatorId;
	}

	public String getStartDateTime() {
		return startDateTime;
	}

	public String getEndDateTime() {
		return endDateTime;
	}

	public Long getDistributorId() {
		return distributorId;
	}

	public String getOrderId() {
		return orderId;
	}

	public boolean isCashStatusInProgress() {
		return cashStatusInProgress;
	}

	public Integer getCashStatusExact() {
		return cashStatusExact;
	}
}
