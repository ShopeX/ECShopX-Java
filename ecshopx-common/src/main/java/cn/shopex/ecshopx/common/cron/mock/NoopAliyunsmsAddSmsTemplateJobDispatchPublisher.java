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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsTemplateJobDispatchPublisher;

/**
 * test-cron 下覆盖真实 {@link AliyunsmsAddSmsTemplateJobDispatchPublisher}，避免经 Bus 的外呼链。
 */
public class NoopAliyunsmsAddSmsTemplateJobDispatchPublisher implements AliyunsmsAddSmsTemplateJobDispatchPublisher {

	public static final String SMS_NOOP_TEMPLATE_CODE = "SMS_NOOP_TEMPLATE_CODE";

	@Override
	public String publish(
			long companyId,
			int templateType,
			String templateName,
			String remark,
			String templateContent,
			int sceneId,
			String relatedSignName) {
		return SMS_NOOP_TEMPLATE_CODE;
	}
}
