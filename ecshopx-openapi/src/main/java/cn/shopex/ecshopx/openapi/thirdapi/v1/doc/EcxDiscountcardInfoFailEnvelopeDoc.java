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
 * <p>本接口业务失败全部走 {@code OpenapiLegacyZeroCodeFailException} →
 * {@code OpenapiExceptionHandler#handleLegacyZeroCodeFail} → code=E0000 但 status=fail（PHP 旧契约）、data=null：
 * card_id 缺失/非整数「获取卡券的详细信息出错.」；券不存在「该优惠券已失效」；详情为空「获取失败.」。
 * 鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxDiscountcardInfoFailEnvelope")
public class EcxDiscountcardInfoFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0000 且 status=fail 为业务失败（PHP 旧契约）；E4004 未授权", example = "E0000")
	public String code;

	@Schema(description = "错误说明", example = "该优惠券已失效")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}