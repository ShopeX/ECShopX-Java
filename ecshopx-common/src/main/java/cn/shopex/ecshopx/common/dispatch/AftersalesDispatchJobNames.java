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

public final class AftersalesDispatchJobNames {

	public static final String REFUND_JOB = "job:15:AftersalesBundle\\Jobs\\RefundJob";

	public static final String SEND_AFTER_SALE_WAIT_DEAL_NOTICE =
			"job:17:WorkWechatBundle\\Jobs\\sendAfterSaleWaitDealNoticeJob";

	public static final String SEND_AFTER_SALE_WAIT_CONFIRM_NOTICE_JOB =
			"job:24:WorkWechatBundle\\Jobs\\sendAfterSaleWaitConfirmNoticeJob";

	public static final String SEND_AFTER_SALE_CANCEL_NOTICE_JOB =
			"job:26:WorkWechatBundle\\Jobs\\sendAfterSaleCancelNoticeJob";

	public static final String ORDER_REFUND_COMPLETE_JOB =
			"job:19:AftersalesBundle\\Jobs\\OrderRefundCompleteJob";

	public static final String AFTERSALES_SUCCESS_SEND_MSG_JOB =
			"job:21:AftersalesBundle\\Jobs\\AftersalesSuccessSendMsg";

	private AftersalesDispatchJobNames() {
	}
}
