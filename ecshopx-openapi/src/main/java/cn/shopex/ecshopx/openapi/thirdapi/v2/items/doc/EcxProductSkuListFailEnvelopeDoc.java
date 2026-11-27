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
 * E0001（ResourceException → BUSINESS_ERROR，OpenapiPaginationParams）：
 * 「page参数必填」（page 缺失/null/非整数/≤0）/「page_size参数必填」（page_size 缺失/null/非整数）。
 * E5320（OpenapiItemsV2FailException → handleItemsV2Fail，ORDER_AFTERSALES_HANDLE_ERROR）：
 * 「开始时间不能大于结束时间」（time_begin/time_end 均 PHP truthy 且解析后起点>终点）。
 * DB 等未处理异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxProductSkuListFailEnvelope")
public class EcxProductSkuListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 分页参数缺失/非法；E5320 时间窗非法；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "page参数必填")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}