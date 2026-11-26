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
 * E5001：未填写付费等级名称 / 未填写会员折扣 / 购买金额必填一项 / 请求参数错误（金额或折扣非法）；
 * E5542「该类型下会员付费等级已存在」（同 lv_type 已有等级）；
 * E5540「最多只能创建2个会员付费等级」（vip+svip 均已存在）；
 * E0001「付费等级数据序列化失败」（JSON 序列化异常，ResourceException 通道）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxVipGradeCreateFailEnvelope")
public class EcxVipGradeCreateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5542 类型已存在；E5540 数量上限；E0001 序列化失败；E4004 未授权",
			example = "E5540")
	public String code;

	@Schema(description = "错误说明", example = "最多只能创建2个会员付费等级")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}