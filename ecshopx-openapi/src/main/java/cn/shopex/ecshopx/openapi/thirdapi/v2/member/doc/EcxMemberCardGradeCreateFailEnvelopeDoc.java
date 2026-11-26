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
 * E5001：未填写等级名称 / 未填写会员折扣 / 未填写升级条件 / 请求参数错误（discount 越界或消费门槛非法）；
 * E5530「最多只能创建5个会员等级」（租户等级计数 ≥6，含默认等级）；
 * E0001「等级数据序列化失败」（JSON 序列化异常，ResourceException 通道）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberCardGradeCreateFailEnvelope")
public class EcxMemberCardGradeCreateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5530 等级数量上限；E0001 序列化失败；E4004 未授权",
			example = "E5530")
	public String code;

	@Schema(description = "错误说明", example = "最多只能创建5个会员等级")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}