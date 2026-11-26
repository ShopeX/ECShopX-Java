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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E5001：请填写正确的储值规则ID / 需付金额必填 / 请填写正确的充值类型 / 请填写正确的赠送金额；
 * E5580：请填写正确的充值金额 / 修改的充值规则不存在（定位查无/UPDATE affected=0/更新后回读为空）/
 * 当前面额数已存在，不能重复添加（与其他规则冲突）/ 底层归一化文案透传。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxRechargeRuleUpdateFailEnvelope")
public class EcxRechargeRuleUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数必填/格式；E5580 金额范围/规则不存在/面额冲突；E4004 未授权",
			example = "E5580")
	public String code;

	@Schema(description = "错误说明", example = "修改的充值规则不存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}