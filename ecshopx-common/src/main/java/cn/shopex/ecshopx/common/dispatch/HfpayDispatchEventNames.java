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

public final class HfpayDispatchEventNames {

	public static final String EVENT_HFPAY_PROFIT_SHARING =
			"event:267:HfPayBundle\\Events\\HfpayProfitSharingEvent";

	public static final String LISTENER_HFPAY_PROFIT_SHARING =
			"listener:HfPayBundle\\Listeners\\ProfitSharing";

	public static final String EVENT_HFPAY_PROFIT_SHARING_TRADE_RECORD =
			"event:295:HfPayBundle\\Events\\HfpayProfitSharingEvent";

	public static final String LISTENER_HFPAY_TRADE_RECORD_PROFIT_SHARING =
			"listener:HfPayBundle\\Listeners\\HfpayTradeRecordListener@profit";

	public static final String EVENT_HFPAY_DISTRIBUTOR_WITHDRAW =
			"event:268:HfPayBundle\\Events\\HfPayDistributorWithdrawEvent";

	public static final String LISTENER_HFPAY_DISTRIBUTOR_WITHDRAW =
			"listener:HfPayBundle\\Listeners\\DistributorWithdrawListener";

	public static final String EVENT_HFPAY_POPULARIZE_WITHDRAW =
			"event:270:HfPayBundle\\Events\\HfPayPopularizeWithdrawEvent";

	public static final String LISTENER_HFPAY_POPULARIZE_WITHDRAW =
			"listener:HfPayBundle\\Listeners\\PopularizeWithdrawListener";

	public static final String EVENT_HFPAY_REFUND_SUCCESS =
			"event:294:HfPayBundle\\Events\\HfpayRefundSuccessEvent";

	public static final String LISTENER_HFPAY_TRADE_RECORD_HFPAY_REFUND_SUCCESS =
			"listener:HfPayBundle\\Listeners\\HfpayTradeRecordListener@refundSuccess";

	public static final String EVENT_HFPAY_DISTRIBUTOR_WITHDRAW_SUCCESS =
			"event:296:HfPayBundle\\Events\\HfPayDistributorWithdrawSuccessEvent";

	public static final String LISTENER_HFPAY_TRADE_RECORD_DISTRIBUTOR_WITHDRAW_SUCCESS =
			"listener:HfPayBundle\\Listeners\\HfpayTradeRecordListener@withdraw";

	private HfpayDispatchEventNames() {}
}
