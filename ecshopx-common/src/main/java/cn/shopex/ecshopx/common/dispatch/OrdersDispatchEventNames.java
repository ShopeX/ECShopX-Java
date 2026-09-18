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

public final class OrdersDispatchEventNames {

	public static final String EVENT_NORMAL_ORDER_CANCEL = "event:orders:normal_order_cancel";

	public static final String EVENT_NORMAL_ORDER_ADD = "event:orders:normal_order_add";

	/** Bus message name aligned with CSV fullname event:288:OrdersBundle\\Events\\NormalOrderPaySuccessEvent. */
	public static final String EVENT_NORMAL_ORDER_PAY_SUCCESS =
			"event:288:OrdersBundle\\Events\\NormalOrderPaySuccessEvent";

	public static final String EVENT_NORMAL_ORDER_DELIVERY = "event:orders:normal_order_delivery";

	/** Second listener registered for normal-order-add fan-out (after supplier split). */
	public static final String LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_ADD = "listener:youshu.orders_normal_order_add";

	/** Third listener: push new normal order snapshot to marketing center after supplier and youshu listeners. */
	public static final String LISTENER_THIRDPARTY_ORDER_ADD_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_ADD =
			"listener:thirdparty.order_add_push_marketing_center_on_normal_order_add";

	public static final String LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_PAY_SUCCESS =
			"listener:youshu.orders_normal_order_pay_success";

	public static final String LISTENER_WSUGC_BUNDLE_ORDERS_NORMAL_ORDER_PAY_SUCCESS =
			"listener:WsugcBundle\\Listeners\\Order";

	public static final String LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_DELIVERY =
			"listener:youshu.orders_normal_order_delivery";

	public static final String LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY =
			"listener:thirdparty.order_delivery_push_marketing_center_on_normal_order_delivery";

	public static final String LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY =
			"listener:thirdparty.order_delivery_push_dm_crm_on_normal_order_delivery";

	/** Synchronous trade-finish bridge: marks mall normal orders paid, then publishes normal-order pay-success. */
	public static final String LISTENER_ORDERS_TRADE_FINISH_NORMAL_ORDER_PAY_SUCCESS_BRIDGE =
			"listener:orders.trade_finish_normal_order_pay_success_bridge";

	public static final String LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_CANCEL = "listener:youshu.orders_normal_order_cancel";

	public static final String EVENT_WX_ORDER_SHIPPING = "event:orders:wx_order_shipping";

	/** Bus message name aligned with CSV fullname event:291:OrdersBundle\\Events\\TradeFinishEvent. */
	public static final String EVENT_TRADE_FINISH = "event:291:OrdersBundle\\Events\\TradeFinishEvent";

	/**
	 * Second trade-finish bus plane for admin offline bank-transfer approval ({@code do_check} success); same listener
	 * fan-out as {@link #EVENT_TRADE_FINISH} with a distinct bus message name for that entry surface.
	 */
	public static final String EVENT_TRADE_FINISH_CSV292 = "event:292:OrdersBundle\\Events\\TradeFinishEvent";

	/** Listener name aligned with CSV listener FQN + method for trade-finish Hfpay pay-success fan-out. */
	public static final String LISTENER_HFPAY_TRADE_RECORD_TRADE_FINISH_PAY_SUCCESS =
			"listener:HfPayBundle\\Listeners\\HfpayTradeRecordListener@paySuccess";

	public static final String EVENT_ORDER_PROCESS_LOG = "event:orders:order_process_log";

	public static final String EVENT_NORMAL_ORDER_CONFIRM_RECEIPT = "event:orders:normal_order_confirm_receipt";

	public static final String LISTENER_YOUSHU_ORDERS_NORMAL_ORDER_CONFIRM_RECEIPT =
			"listener:youshu.orders_normal_order_confirm_receipt";

	/** Push normal-order confirm-receipt snapshot to marketing center (fan-out after youshu listener). */
	public static final String LISTENER_THIRDPARTY_ORDER_CONFIRM_RECEIPT_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_CONFIRM_RECEIPT =
			"listener:thirdparty.order_confirm_receipt_push_marketing_center_on_normal_order_confirm_receipt";

	/**
	 * Shopex CRM sync on normal-order confirm receipt; aligns PHP ThirdPartyBundle ShopexCrm SyncConfirmReceiptOrder on
	 * EVENT_NORMAL_ORDER_CONFIRM_RECEIPT.
	 */
	public static final String LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_CONFIRM_RECEIPT_ORDER_ON_NORMAL_ORDER_CONFIRM_RECEIPT =
			"listener:thirdparty.shopex_crm.sync_confirm_receipt_order_on_normal_order_confirm_receipt";

	/** 数云开放平台：支付成功 → trade.sync */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_TRADE_SYNC_ON_NORMAL_ORDER_PAY_SUCCESS =
			"listener:shuyun.open_platform.trade_sync_on_normal_order_pay_success";

	/** 数云开放平台：发货 → trade.sync */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_TRADE_SYNC_ON_NORMAL_ORDER_DELIVERY =
			"listener:shuyun.open_platform.trade_sync_on_normal_order_delivery";

	/** 数云开放平台：确认收货 → trade.sync */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_TRADE_SYNC_ON_NORMAL_ORDER_CONFIRM_RECEIPT =
			"listener:shuyun.open_platform.trade_sync_on_normal_order_confirm_receipt";

	/** 数云开放平台：取消（已支付）→ trade.sync */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_TRADE_SYNC_ON_NORMAL_ORDER_CANCEL =
			"listener:shuyun.open_platform.trade_sync_on_normal_order_cancel";

	/** 数云开放平台：支付成功 → 线下权益 result.push.v2 USED */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_OFFLINE_BENEFIT_CONSUME_ON_NORMAL_ORDER_PAY_SUCCESS =
			"listener:shuyun.open_platform.offline_benefit_consume_on_normal_order_pay_success";

	/** 数云开放平台：取消 → 线下权益 result.push.v2 NOT_USED */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_OFFLINE_BENEFIT_CONSUME_ON_NORMAL_ORDER_CANCEL =
			"listener:shuyun.open_platform.offline_benefit_consume_on_normal_order_cancel";

	private OrdersDispatchEventNames() {
	}
}
