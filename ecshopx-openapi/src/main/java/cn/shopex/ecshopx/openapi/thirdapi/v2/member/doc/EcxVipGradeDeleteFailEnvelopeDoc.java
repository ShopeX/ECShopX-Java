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
 * E5001「请求参数错误」（vip_grade_id null/空串）；
 * E5541「会员付费等级找不到」（trim+parseLong/整值 double 解析后等值查无，非法字符串按 -1 查询必查无）；
 * E5543「会员付费等级删除错误」（默认等级禁删）/
 * E5543「会员付费等级无法删除，该会员付费等级下扔存在关联的会员」（源码原文含错字「扔」，PHP 对齐保留；
 * 关联计数不过滤 end_date，过期关联也阻止删除）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxVipGradeDeleteFailEnvelope")
public class EcxVipGradeDeleteFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5541 等级找不到；E5543 默认等级/存在关联会员禁删；E4004 未授权",
			example = "E5543")
	public String code;

	@Schema(description = "错误说明", example = "会员付费等级无法删除，该会员付费等级下扔存在关联的会员")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}