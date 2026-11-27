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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null，经
 * {@code OpenapiExceptionHandler#handleItemsV2Fail}（OpenapiItemsV2FailException）：
 * E5002「商品信息格式错误」（sku_list 空/非法 JSON/非数组/元素非对象）；
 * E5001「商品信息的货号必填」（元素缺 sku_id 或空白）/
 * 「商品信息的库存为0-999999999的整数」（缺 stock、非整数、负数或 >999999999）；
 * E5101「商品找不到」（GOODS_NOT_FOUND——全部货号均查不到；部分未命中不报错）。
 * 逐行更新失败不产生本信封（记入成功 data.update_error）。
 * 未捕获异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxProductStockUpdateFailEnvelope")
public class EcxProductStockUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E5002 格式错误；E5001 货号/库存非法；E5101 商品找不到；E4004 未授权",
			example = "E5001")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "商品信息的库存为0-999999999的整数")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}