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
 * E5001：会员等级ID参数错误（两键均缺席或 grade_id 非严格整数）/
 * 会员等级ID与付费会员等级ID不能同时存在 / 付费会员等级ID参数错误（vip_grade_id 非严格整数）；
 * E5002「每页的大小最大为500！」（page_size 原始解析值 ≥500）。
 * 本服务无 E5000 兜底 catch：DB 等未处理异常经 GlobalExceptionHandler → HTTP 500
 * {"code":500,"msg":"服务器内部错误"}（ApiResult 形态，非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberCardGradeRelListFailEnvelope")
public class EcxMemberCardGradeRelListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 等级ID参数错误/互斥校验；E5002 分页格式错误；E4004 未授权",
			example = "E5001")
	public String code;

	@Schema(description = "错误说明", example = "会员等级ID与付费会员等级ID不能同时存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}