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
 * E5001：商品货号参数错误 / 同步库存数参数错误 / 店铺ID参数错误；
 * E5101「商品找不到」/ E5401「店铺找不到」；
 * E5410「商品总部发货无法更新库存和价格」（店铺行/SPU 行 is_total_store 生效）；
 * E5000（其他异常 message 透传，空则「未知错误」）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxItemStoreSyncFailEnvelope")
public class EcxItemStoreSyncFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5101 商品找不到；E5401 店铺找不到；E5410 总部发货限制；E5000 未知错误；E4004 未授权",
			example = "E5001")
	public String code;

	@Schema(description = "错误说明", example = "同步库存数参数错误")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}