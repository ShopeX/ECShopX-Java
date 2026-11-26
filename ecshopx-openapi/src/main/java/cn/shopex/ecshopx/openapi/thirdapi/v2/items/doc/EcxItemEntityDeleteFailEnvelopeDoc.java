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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E5001「商品货号必填」；E5103（GOODS_DELETE_ERROR）：商品删除错误（默认 SKU 查无 / 兜底文案）/
 * 删除商品信息有误 / 店铺商品信息有误，不可删除（distributor_id 不匹配，本接口固定按平台 0 删除）/
 * 删除链路其他异常 message 透传。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxItemEntityDeleteFailEnvelope")
public class EcxItemEntityDeleteFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 商品货号必填；E5103 商品删除错误/信息有误/店铺不匹配；E4004 未授权", example = "E5103")
	public String code;

	@Schema(description = "错误说明", example = "商品删除错误")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}