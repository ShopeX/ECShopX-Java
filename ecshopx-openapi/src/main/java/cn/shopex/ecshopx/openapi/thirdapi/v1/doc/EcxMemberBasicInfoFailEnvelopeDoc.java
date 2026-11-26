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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。
 *
 * <p>业务失败均为 {@code ResourceException} → {@code OpenapiExceptionHandler#handleResource} → E0001、data=null：
 * 请填写unionid（body unionid 非字符串）/ 会员id（body external_member_id 非字符串）/
 * 请填写正确的手机号（mobile 提供但格式非法）/ unionid或者手机号或会员id必填（三者均 PHP-empty）。
 * 注意：查无会员不是失败，返回成功信封且 data=[]。
 * 鉴权失败：E4004「未授权」（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxMemberBasicInfoFailEnvelope")
public class EcxMemberBasicInfoFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 参数校验失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "unionid或者手机号或会员id必填")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}