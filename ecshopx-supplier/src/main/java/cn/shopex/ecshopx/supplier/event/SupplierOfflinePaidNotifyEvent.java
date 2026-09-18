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

package cn.shopex.ecshopx.supplier.event;

import org.springframework.context.ApplicationEvent;

public class SupplierOfflinePaidNotifyEvent extends ApplicationEvent {

	private final long companyId;
	private final long orderId;
	private final long userId;

	public SupplierOfflinePaidNotifyEvent(Object source, long companyId, long orderId, long userId) {
		super(source);
		this.companyId = companyId;
		this.orderId = orderId;
		this.userId = userId;
	}

	public long getCompanyId() {
		return companyId;
	}

	public long getOrderId() {
		return orderId;
	}

	public long getUserId() {
		return userId;
	}
}
