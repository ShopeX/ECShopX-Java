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
 * 仅 OpenAPI 文档；POST 请求 JSON body，1 键与 query 同名双通道（{@code originalString}：
 * body 含键优先、值 null 视为未传，取值不 trim；服务内 trim 后匹配）。
 * 查询见 {@code OpenapiThirdApiV2RefundDetailService#getRefundDetail}
 * （company_id + refund_bn 等值，无 LIMIT 1——多行命中抛 TooManyResults → E5340）。
 */
@Schema(
		name = "EcxRefundDetailGetBody",
		description = "退款单详情参数（1 键，body 含键优先否则 query 同名键）。按租户查 aftersales_refund。")
public class EcxRefundDetailGetBodyDoc {

	@Schema(
			description =
					"退款单号；必填（非空否则 E5001「请填写退款单号」）。trim 后按 refund_bn 等值匹配"
							+ "（bigint 列，MySQL 隐式转换）；查无 → E5340「Undefined array key \\\"refund_channel\\\"」"
							+ "（PHP 错误文案对齐契约）；同租户多行命中 → E5340（TooManyResults 异常 message）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "2202608258337278667")
	public String refund_bn;
}