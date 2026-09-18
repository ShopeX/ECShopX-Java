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

public final class OrdersDispatchJobNames {

	public static final String INVOICE_RED_JOB = "job:20:OrdersBundle\\Jobs\\InvoiceRedJob";

	public static final String TRADE_REFUND_STATISTICS_JOB = "job:66:OrdersBundle\\Jobs\\TradeRefundStatistics";

	public static final String SEND_PAY_ORDERS_REMIND_JOB =
			"job:115:OrdersBundle\\Jobs\\SendPayOrdersRemindJob";

	public static final String REFUND_BY_ORDER_UPDATE_ORDER_STATUS_JOB =
			"job:116:OrdersBundle\\Jobs\\RefundByOrderUpdateOrderStatus";

	public static final String GENERATE_STATEMENTS_JOB =
			"job:118:OrdersBundle\\Jobs\\GenerateStatementsJob";

	public static final String INVOICE_PUSH_OMS_JOB =
			"job:120:OrdersBundle\\Jobs\\InvoicePushOmsJob";

	public static final String SEND_INVOICE_EMAIL_JOB =
			"job:122:OrdersBundle\\Jobs\\SendInvoiceEmailJob";

	public static final String INVOICE_CREATE_JOB =
			"job:124:OrdersBundle\\Jobs\\InvoiceCreateJob";

	public static final String INVOICE_RED_QUERY_JOB =
			"job:125:OrdersBundle\\Jobs\\InvoiceRedQueryJob";

	public static final String INVOICE_QUERY_JOB =
			"job:126:OrdersBundle\\Jobs\\InvoiceQueryJob";

	public static final String FINISH_ORDER_JOB =
			"job:128:OrdersBundle\\Jobs\\FinishOrderJob";

	public static final String CONSUMPTION_ORDER_JOB =
			"job:129:OrdersBundle\\Jobs\\ConsumptionOrderJob";

	private OrdersDispatchJobNames() {
	}
}
