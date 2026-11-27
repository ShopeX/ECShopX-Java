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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null，经
 * {@code OpenapiExceptionHandler#handleResource}（ResourceException → BUSINESS_ERROR=E0001）：
 * 「每页的大小最大为500！」（page_size 原始解析值 ≥500）——本服务唯一可达业务失败文案
 * （与增量接口不同：本分页解析器不校验非整数文本，非法文本静默回退默认值）。
 * 不可达文案：「当前页面最小值为1」（page 已 max(…,1)）/「每页显示数量1-500」（page_size 已 max(…,20) 且 ≥500 先拦）。
 * mobile 查无会员不报错（成功空数组）。DB 等未处理异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrdersSoldGetFailEnvelope")
public class EcxOrdersSoldGetFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 分页大小超限；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "每页的大小最大为500！")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}