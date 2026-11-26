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
 * <p>本接口无入参、无业务校验失败分支；可达失败为鉴权类：
 * 直连 internal 无鉴权上下文 → E4004「未授权」、data=null；
 * 公网网关签名失败另有 E4001/E4002/E4003（data 为回显请求参数 Map）。
 */
@Schema(name = "EcxMemberTagLibraryFailEnvelope")
public class EcxMemberTagLibraryFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E4004 未授权/签名错误；网关侧另有 E4001 缺参、E4002 timestamp、E4003 app_key", example = "E4004")
	public String code;

	@Schema(description = "错误说明", example = "未授权")
	public String message;

	@Schema(description = "附加数据：handler 侧失败为 null；网关签名失败时为回显的请求参数 Map", nullable = true)
	public Object data;
}