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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。
 *
 * <p>业务失败：plat_account 缺失/空白 → E5001「商派会员Id参数错误」
 * （{@code OpenapiThirdApiV2DiscountCardUserDiscountListParams}）；
 * page_size 解析后 ≥500（含 500）→ E5002「每页的大小最大为500！」
 * （{@code OpenapiThirdApiV2DiscountCardListParams}）；
 * 会员查无/无手机号 → E5501「商派会员Id错误，未查询到会员信息」
 * （{@code OpenapiThirdApiV2DiscountCardGetUserDiscountListService}）。
 * 均 {@code OpenapiKaquanV2FailException} → data=null。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxUserdiscountListFailEnvelope")
public class EcxUserdiscountListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description =
					"业务码：E5001 商派会员Id参数错误；E5002 每页的大小最大为500！；"
							+ "E5501 商派会员Id错误，未查询到会员信息；E4004 未授权",
			example = "E5501")
	public String code;

	@Schema(description = "错误说明", example = "商派会员Id错误，未查询到会员信息")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}