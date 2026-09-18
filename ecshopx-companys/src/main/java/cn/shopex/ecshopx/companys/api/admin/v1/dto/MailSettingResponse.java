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

package cn.shopex.ecshopx.companys.api.admin.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GET /mail/setting 响应：7 字段，序列化为 PHP 对齐的大写 Redis key。EMAIL_PASSWORD 非空时加密后返回
 * （契约 §11.2），空则空串。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailSettingResponse {

	@JsonProperty("EMAIL_SMTP_PORT")
	private String emailSmtpPort;

	@JsonProperty("EMAIL_RELAY_HOST")
	private String emailRelayHost;

	@JsonProperty("EMAIL_SENDER")
	private String emailSender;

	@JsonProperty("EMAIL_USER")
	private String emailUser;

	@JsonProperty("EMAIL_PASSWORD")
	private String emailPassword;

	@JsonProperty("EMAIL_ACTIVATION_H5_DOMAIN")
	private String emailActivationH5Domain;

	@JsonProperty("EMAIL_ACTIVATION_PC_DOMAIN")
	private String emailActivationPcDomain;
}
