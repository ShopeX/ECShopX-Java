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
 * <p>整体失败均为入参校验类 {@code ResourceException} → {@code OpenapiExceptionHandler#handleResource} →
 * E0001、data=null（sku_list 必填且格式正确/库存数据为空/sku_list 单次最多 N 条/stock 必须为 0-999999999 的整数/shop_code 必填）。
 * 行级业务失败不走本信封（见成功 data.update_error）。
 * 鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxDigiosStockPushFailEnvelope")
public class EcxDigiosStockPushFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 入参校验失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "sku_list 必填且格式正确")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}