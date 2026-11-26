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
 * E5002「每页的大小最大为500！」（page_size 原始解析值 ≥500）；
 * E5320「开始时间不能大于结束时间」（PHP 旧契约复用售后错误码）；
 * E5000：category_id 过滤触发不存在列 SQL 错误 / DB 异常 message 透传 / brand_id 非数字解析异常 message 透传。
 * E5001「当前页面最小值为1」「每页显示数量1-500」分支在 max() 钳制后不可达。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxItemsEntityListFailEnvelope")
public class EcxItemsEntityListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5002 分页格式错误；E5320 时间区间错误；E5000 SQL/解析异常；E4004 未授权",
			example = "E5002")
	public String code;

	@Schema(description = "错误说明", example = "每页的大小最大为500！")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}