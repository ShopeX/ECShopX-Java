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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），三种形态：
 * <ul>
 *   <li>E1001、data=[]：控制器直接 fail —— 清空模式需要提供 user_id / 标签关系数据不能为空。</li>
 *   <li>E1001（未找到会员，清空模式）或 E9999（标签关系推送失败：{异常信息} / 清空会员标签失败：{异常信息}）：
 *       {@code OpenapiMemberTagRelationPushFailException} → handleMemberTagRelationPushFail → data=[]。</li>
 *   <li>鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。</li>
 * </ul>
 * 正常模式行级失败（查无会员/系统标签不可编辑）不走本信封，计入成功 data.fail_count。
 */
@Schema(name = "EcxMemberTagRelationPushFailEnvelope")
public class EcxMemberTagRelationPushFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E1001 入参/未找到会员；E9999 系统异常；E4004 未授权", example = "E1001")
	public String code;

	@Schema(description = "错误说明", example = "标签关系数据不能为空")
	public String message;

	@ArraySchema(
			arraySchema = @Schema(description = "业务失败时恒为空数组 []；鉴权失败时为 null"),
			schema = @Schema(nullable = true))
	public List<Object> data;
}