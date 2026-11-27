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
 * E5320「开始时间不能大于结束时间」（ORDER_AFTERSALES_HANDLE_ERROR——time_begin/time_end 均 PHP truthy
 * 且解析后起点>终点；本接口复用售后错误码，按现状记录）。本服务唯一业务失败码。
 * page≤0 时负 offset → SQL 异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxProductGoodsListFailEnvelope")
public class EcxProductGoodsListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5320 时间窗非法；E4004 未授权", example = "E5320")
	public String code;

	@Schema(description = "错误说明", example = "开始时间不能大于结束时间")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}