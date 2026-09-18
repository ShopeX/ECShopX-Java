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

public final class AliyunsmsDispatchJobNames {

	public static final String MODIFY_SMS_SIGN_JOB = "job:49:AliyunsmsBundle\\Jobs\\ModifySmsSign";

	public static final String ADD_SMS_SIGN_JOB = "job:48:AliyunsmsBundle\\Jobs\\AddSmsSign";

	public static final String DELETE_SMS_SIGN_JOB = "job:50:AliyunsmsBundle\\Jobs\\DeleteSmsSign";

	public static final String QUERY_SMS_SIGN_JOB = "job:51:AliyunsmsBundle\\Jobs\\QuerySmsSign";

	public static final String ADD_SMS_BATCH_RECORD_JOB = "job:52:AliyunsmsBundle\\Jobs\\AddSmsBatchRecord";

	public static final String QUERY_SEND_DETAIL_JOB = "job:53:AliyunsmsBundle\\Jobs\\QuerySendDetail";

	public static final String ADD_SMS_TEMPLATE_JOB = "job:54:AliyunsmsBundle\\Jobs\\AddSmsTemplate";

	public static final String MODIFY_SMS_TEMPLATE_JOB = "job:55:AliyunsmsBundle\\Jobs\\ModifySmsTemplate";

	public static final String DELETE_SMS_TEMPLATE_JOB = "job:56:AliyunsmsBundle\\Jobs\\DeleteSmsTemplate";

	public static final String QUERY_SMS_TEMPLATE_JOB = "job:57:AliyunsmsBundle\\Jobs\\QuerySmsTemplate";

	public static final String SYNC_SMS_SIGNS_JOB = "job:58:AliyunsmsBundle\\Jobs\\SyncSmsSigns";

	public static final String SYNC_SMS_TEMPLATES_JOB = "job:59:AliyunsmsBundle\\Jobs\\SyncSmsTemplates";

	private AliyunsmsDispatchJobNames() {
	}
}
