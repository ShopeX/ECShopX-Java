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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null，经
 * {@code OpenapiExceptionHandler#handleResource}（ResourceException → BUSINESS_ERROR=E0001）：
 * 「请填写unionid」（body unionid 为非字符串非 null 值）/
 * 「请填写正确的手机号」（mobile 键出现但值不匹配 ^1[345789][0-9]{9}$，含 null/空串）/
 * 「unionid或者手机号必填」（两键均 PHP empty）/
 * 「会员信息获取失败」（手机号或 unionid 查不到会员）。
 * page≤0 且启用分页时 OFFSET 为负 → SQL 异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrderListFailEnvelope")
public class EcxOrderListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 业务失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "unionid或者手机号必填")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}