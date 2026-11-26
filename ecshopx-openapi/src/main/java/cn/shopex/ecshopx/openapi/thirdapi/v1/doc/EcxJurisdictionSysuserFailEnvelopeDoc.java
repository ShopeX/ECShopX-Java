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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），两种形态：
 * <ul>
 *   <li>E0001「操作失败」：data 为嵌套对象 {status:false, message:具体原因}
 *      （配置错误 / 帐号已存在（注意「帐」字）/ 保存失败）。</li>
 *   <li>E0000 且 status=fail（PHP 旧契约）：data=null
 *      （账号已开通 / 未查询到更新数据 / admin 开通流程的其他 ResourceException 文案）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxJurisdictionSysuserFailEnvelope")
public class EcxJurisdictionSysuserFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E0001 业务失败（data 为嵌套失败对象）；E0000 且 status=fail 旧契约失败（data=null）；E4004 未授权",
			example = "E0001")
	public String code;

	@Schema(description = "错误说明（E0001 时固定「操作失败」，具体原因在 data.message）", example = "操作失败")
	public String message;

	@Schema(description = "附加数据：E0001 时为 {status:false, message}；旧契约/鉴权失败为 null", nullable = true)
	public NestedFailDoc data;

	@Schema(name = "EcxJurisdictionSysuserNestedFail", description = "嵌套失败对象")
	public static class NestedFailDoc {

		@Schema(description = "恒为 false", example = "false")
		public Boolean status;

		@Schema(description = "具体失败原因：配置错误 / 帐号已存在 / 保存失败", example = "帐号已存在")
		public String message;
	}
}