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
 * 仅 OpenAPI 文档；非 200 错误体（ApiResult 形态，非 OpenapiEnvelope）。来源
 * {@code GlobalExceptionHandler}（@Order 最低优先级兜底，OpenapiExceptionHandler 无对应映射）：
 * BadRequestException → HTTP 400 {@code {"code":400,"msg":"参数错误"}}（本接口触发条件：
 * company_id > Integer.MAX，见 CompanyLogisticsListService）；
 * 其他未处理异常 → HTTP 500 {@code {"code":500,"msg":"服务器内部错误"}}。
 * data 为 null 时因 @JsonInclude(NON_NULL) 整键省略。
 */
@Schema(name = "EcxLogisticsEnabledGetErrorBody")
public class EcxLogisticsEnabledGetErrorBodyDoc {

	@Schema(description = "HTTP 状态同值错误码（400 参数错误 / 500 服务器内部错误）", example = "400")
	public Integer code;

	@Schema(description = "错误说明", example = "参数错误")
	public String msg;
}