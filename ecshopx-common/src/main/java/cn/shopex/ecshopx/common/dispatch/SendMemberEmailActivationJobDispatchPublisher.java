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

/**
 * 邮箱注册成功后异步发送激活链接邮件的 job 发布者（default 队列）。
 */
@FunctionalInterface
public interface SendMemberEmailActivationJobDispatchPublisher {

	void publish(long companyId, String email, String clientIp, String deviceId, String activationBaseUrl);
}
