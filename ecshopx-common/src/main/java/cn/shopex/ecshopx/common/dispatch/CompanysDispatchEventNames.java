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

public final class CompanysDispatchEventNames {

	public static final String EVENT_COMPANY_CREATE = "event:companys:company_create";

	/** 对齐 PHP {@code CompanysBundle\\Events\\OperatorCreateEvent} */
	public static final String EVENT_OPERATOR_CREATE = "event:CompanysBundle\\Events\\OperatorCreateEvent";

	public static final String LISTENER_DIGIOS_ACCOUNT_SYNC_ON_OPERATOR_CREATE =
			"listener:digios.account_sync_on_operator_create";

	/** POS 门店账号新建成功 */
	public static final String EVENT_POS_STORE_ACCOUNT_CREATE = "event:companys:pos_store_account_create";

	public static final String LISTENER_DIGIOS_ACCOUNT_SYNC_ON_POS_STORE_ACCOUNT_CREATE =
			"listener:digios.account_sync_on_pos_store_account_create";

	private CompanysDispatchEventNames() {
	}
}
