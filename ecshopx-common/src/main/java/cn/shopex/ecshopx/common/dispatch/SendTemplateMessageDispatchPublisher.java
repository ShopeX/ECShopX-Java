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

import java.util.Map;

public interface SendTemplateMessageDispatchPublisher {

	/**
	 * Enqueues {@link WechatDispatchJobNames#SEND_TEMPLATE_MESSAGE_JOB} using the payload shape required
	 * by that job handler (canonical keys: {@code company_id}, {@code template_id}, {@code touser}, {@code msg_data}).
	 */
	void publish(long companyId, String templateId, String touser, Map<String, Object> msgData);
}
