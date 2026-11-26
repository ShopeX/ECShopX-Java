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
 * <ul>
 * <li>E5001：货号必填 / 状态必填,且必须是 onsale 或 instock（文案末尾含空格）；</li>
 * <li>E5100：请填写正确的货号格式 / 商品状态可更改的最大数量为1000；</li>
 * <li>E5101「商品找不到」（全部货号查无默认 SKU）；</li>
 * <li>E0000（PHP 旧契约 fail，OpenapiErrorCode.SUCCESS）：供应商商品不可售 /
 *     处方药商品需要开启医药行业配置才能上架 / 处方药商品【{商品名}】审核通过后才能上架
 *     （{@code GoodsItemsBatchApproveStatusService} ResourceException 转码）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxItemEntityStatusUpdateFailEnvelope")
public class EcxItemEntityStatusUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 必填/状态非法；E5100 货号格式/超量；E5101 商品找不到；E0000 上架守卫（PHP 旧契约 fail）；E4004 未授权",
			example = "E5001")
	public String code;

	@Schema(description = "错误说明", example = "供应商商品不可售")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}