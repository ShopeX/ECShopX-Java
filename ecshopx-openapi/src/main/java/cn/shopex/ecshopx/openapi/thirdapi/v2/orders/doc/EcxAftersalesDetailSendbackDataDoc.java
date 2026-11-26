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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；消费者回寄物流（aftersales.sendback_data JSON 解析，未提交/解析失败/"0" 时为 null）。
 * 固定 4 键，写入方 {@code AftersalesFrontWxappSendbackService} 与 {@code AftersalesSendbackService} 一致。
 */
@Schema(name = "EcxAftersalesDetailSendbackData")
public class EcxAftersalesDetailSendbackDataDoc {

	@Schema(description = "物流公司编码（trim 后）", example = "SF")
	public String corp_code;

	@Schema(description = "物流单号（trim 后）", example = "1234567890")
	public String logi_no;

	@Schema(description = "收件地址（可选，缺省空串）", example = "")
	public String receiver_address;

	@Schema(description = "收件电话（可选，缺省空串）", example = "")
	public String receiver_mobile;
}