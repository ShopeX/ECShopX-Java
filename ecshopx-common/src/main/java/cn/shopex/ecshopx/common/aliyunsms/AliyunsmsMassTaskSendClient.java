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

package cn.shopex.ecshopx.common.aliyunsms;

/**
 * 群发任务专用 {@code SendSms} 端口（多号码英文逗号拼接，与 PHP {@code AliyunSmsClient::send} 一致）。
 */
public interface AliyunsmsMassTaskSendClient {

	/**
	 * @param templateParamJson 可为 {@code null} 表示不传模板参数 JSON
	 */
	MassTaskSendSmsResult sendMassSms(
			String accessKeyId,
			String accessKeySecret,
			String phoneNumbers,
			String signName,
			String templateCode,
			String templateParamJson);
}
