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
 * 仅 OpenAPI 文档；成功信封。Handler 返回 {@code OrderCancelReasonConfig#allReasonsAsStringKeyMap()}
 * 的固定映射（LinkedHashMap，插入序 "1"→"12"，共 12 键，值为内置文案），
 * {@code OpenapiResponseBodyAdvice} 包装为 {@code OpenapiEnvelope.success(data)}（message 固定「成功」）。
 * 键即 {@code ecx.order.cancel} 的 cancel_reason_id 取值；纯静态配置，无 DB 访问。
 */
@Schema(name = "EcxOrderCancelReasonsGetSuccessEnvelope")
public class EcxOrderCancelReasonsGetSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@Schema(description = "取消原因映射（固定 12 键，键为原因 ID 字符串，插入序 1→12）")
	public DataDoc data;

	/** data 固定 12 键（"1"～"12"），值为 OrderCancelReasonConfig.REASONS 内置文案。 */
	@Schema(name = "EcxOrderCancelReasonsGetData")
	public static class DataDoc {

		@Schema(name = "1", description = "原因 ID 1 的文案", example = "客户现在不想购买")
		public String reason1;

		@Schema(name = "2", description = "原因 ID 2 的文案", example = "客户商品价格较贵")
		public String reason2;

		@Schema(name = "3", description = "原因 ID 3 的文案", example = "客户价格波动")
		public String reason3;

		@Schema(name = "4", description = "原因 ID 4 的文案", example = "客户商品缺货")
		public String reason4;

		@Schema(name = "5", description = "原因 ID 5 的文案", example = "客户重复下单")
		public String reason5;

		@Schema(name = "6", description = "原因 ID 6 的文案", example = "客户订单商品选择有误")
		public String reason6;

		@Schema(name = "7", description = "原因 ID 7 的文案", example = "客户支付方式选择有误")
		public String reason7;

		@Schema(name = "8", description = "原因 ID 8 的文案", example = "客户收货信息填写有误")
		public String reason8;

		@Schema(name = "9", description = "原因 ID 9 的文案", example = "客户发票信息填写有误")
		public String reason9;

		@Schema(name = "10", description = "原因 ID 10 的文案", example = "客户无法支付订单")
		public String reason10;

		@Schema(name = "11", description = "原因 ID 11 的文案", example = "客户长时间未付款")
		public String reason11;

		@Schema(
				name = "12",
				description = "原因 ID 12 的文案（选 12 取消时须另传 cancel_reason 自定义描述）",
				example = "客户其他原因")
		public String reason12;
	}
}