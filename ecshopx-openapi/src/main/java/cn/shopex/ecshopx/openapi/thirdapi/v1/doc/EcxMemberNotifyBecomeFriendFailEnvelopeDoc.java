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
 * <p>业务失败均为 {@code OpenapiNotifyBecomeFriendFailException}（自带业务码）→
 * {@code OpenapiExceptionHandler#handleNotifyBecomeFriendFail} → data=null：
 * E0001 请填写会员unionid / 请填写导购编号 / is_become_friend必须为1；
 * E4002 会员不存在；E4004 该会员未分配给该导购（注意此处 E4004 为业务语义，非鉴权）；
 * E5001 系统错误：{异常信息}。
 * 鉴权失败：E4004「未授权」（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxMemberNotifyBecomeFriendFailEnvelope")
public class EcxMemberNotifyBecomeFriendFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E0001 缺参/标记非法；E4002 会员不存在；E4004 未分配给该导购或未授权；E5001 系统错误",
			example = "E4004")
	public String code;

	@Schema(description = "错误说明", example = "该会员未分配给该导购")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}