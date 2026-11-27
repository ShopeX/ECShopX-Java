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
 * 仅 OpenAPI 文档；成功信封。Handler 固定返回 {@code Map.of("status", true)}，
 * {@code OpenapiResponseBodyAdvice} 包装为 {@code OpenapiEnvelope.success(data)}（message 固定「成功」）。
 * 取消单明细不回传（内部 execute 返回值被丢弃）。
 */
@Schema(name = "EcxOrderCancelSuccessEnvelope")
public class EcxOrderCancelSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@Schema(description = "结果数据")
	public DataDoc data;

	/** data 固定单键 status（布尔 true，Handler 硬编码）。 */
	@Schema(name = "EcxOrderCancelData")
	public static class DataDoc {

		@Schema(description = "取消受理结果（成功恒为布尔 true）", example = "true")
		public Boolean status;
	}
}