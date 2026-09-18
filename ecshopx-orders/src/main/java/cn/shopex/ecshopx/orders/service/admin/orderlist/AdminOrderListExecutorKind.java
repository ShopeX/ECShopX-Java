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

package cn.shopex.ecshopx.orders.service.admin.orderlist;

/** Dispatch target for admin order list queries (order type / class → executor branch). */
public enum AdminOrderListExecutorKind {

	ASSOCIATION,
	SERVICE,
	BARGAIN_NORMAL,
	NORMAL,
	SUPPLIER_ORDER,
	GROUPS_SERVICE,
	GROUPS_NORMAL,
	MEMBERCARD,
	SECKILL_NORMAL,
	SECKILL_SERVICE,
	DRUG_NORMAL,
	SHOPGUIDE_NORMAL,
	POINTSMALL_NORMAL,
	EXCARD_NORMAL,
	COMMUNITY_NORMAL,
	SHOPADMIN_NORMAL,
	EMPLOYEE_PURCHASE_NORMAL
}
