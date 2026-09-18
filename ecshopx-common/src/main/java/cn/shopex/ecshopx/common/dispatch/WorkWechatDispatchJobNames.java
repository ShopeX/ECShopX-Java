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

public final class WorkWechatDispatchJobNames {

	public static final String SEND_TASK_PROGRESS_NOTICE_JOB =
			"job:38:WorkWechatBundle\\Jobs\\sendTaskProgressNoticeJob";

	public static final String SEND_DELIVERY_WAIT_DELIVERY_NOTICE_JOB =
			"job:112:WorkWechatBundle\\Jobs\\sendDeliveryWaitDeliveryNoticeJob";

	public static final String SEND_DELIVERY_WAIT_ZITI_NOTICE_JOB =
			"job:113:WorkWechatBundle\\Jobs\\sendDeliveryWaitZiTiNoticeJob";

	public static final String SEND_WAITING_DELIVERY_NOTICE_JOB =
			"job:114:WorkWechatBundle\\Jobs\\sendWaitingDeliveryNoticeJob";

	private WorkWechatDispatchJobNames() {
	}
}
