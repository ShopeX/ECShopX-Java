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
 * E0001「每页的大小最大为500！」（page_size 原始解析值 ≥500；分页参数复用运费模板解析器，
 * ResourceException → BUSINESS_ERROR=E0001）。
 * 「当前页面最小值为1」「每页显示数量1-500」分支在 max() 钳制后不可达。
 * vip_grade_id 非数字不报错（按 -1 过滤恒空）。
 * 本服务无其他业务失败码；DB 等未处理异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxVipGradeListFailEnvelope")
public class EcxVipGradeListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 分页大小超限；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "每页的大小最大为500！")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}