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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。业务失败均为 {@code OpenapiKaquanV2FailException}
 * （SERVICE_MISSING_PARAMS）→ E5001、data=null。文案全集
 * （{@code OpenapiDiscountCardV2SendPhpMessages}）：
 * 商派会员Id参数错误 / 优惠券ID参数错误 / 卡券来源参数错误 / 会员ID错误，未查询到会员信息 /
 * 领取的优惠券不存在 / 领取优惠券失败，优惠券已过期 / 领取的优惠券失败，库存不足了 /
 * 不可领取！卡券状态非正常 / 不可领取！ / 您已经领过该券，请到个人中心中查看哦~ /
 * 您已经领取该卡券 / 领取该卡券失败 / 领取卡券记录添加失败, company_id 为空。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxDiscountcardSendFailEnvelope")
public class EcxDiscountcardSendFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 全部业务失败；E4004 未授权", example = "E5001")
	public String code;

	@Schema(description = "错误说明（见类注释文案全集）", example = "领取的优惠券失败，库存不足了")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}