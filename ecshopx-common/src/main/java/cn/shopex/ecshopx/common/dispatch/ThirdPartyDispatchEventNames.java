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

public final class ThirdPartyDispatchEventNames {

	public static final String EVENT_TRADE_UPDATE = "event:thirdparty:trade_update";

	public static final String EVENT_TRADE_AFTERSALES_SAAS_ERP = "event:thirdparty:trade_aftersales_saas_erp";

	public static final String EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP =
			"event:thirdparty:trade_aftersales_cancel_saas_erp";

	public static final String EVENT_TRADE_REFUND_CANCEL_SAAS_ERP =
			"event:thirdparty:trade_refund_cancel_saas_erp";

	public static final String EVENT_TRADE_REFUND_FINISH = "event:thirdparty:trade_refund_finish";

	/** 数云开放平台：退款完成 SUCCESS → refund.sync */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_REFUND_SYNC_ON_TRADE_REFUND_FINISH =
			"listener:shuyun.open_platform.refund_sync_on_trade_refund_finish";

	/** 数云开放平台：退款完成 → 按订单最新状态重推 trade.sync */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_TRADE_SYNC_ON_TRADE_REFUND_FINISH =
			"listener:shuyun.open_platform.trade_sync_on_trade_refund_finish";

	private ThirdPartyDispatchEventNames() {}
}
