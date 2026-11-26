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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E5001：请求参数错误：会员手机号参数错误 / 增加积分或减去积分二选一必填 /
 * 增加积分和减去积分只能存在一个 / 请求参数错误：增加积分参数错误 / 请求参数错误：减去积分参数错误 /
 * 请求参数错误：积分变动原因（备注）参数错误 / 请求参数错误：外部唯一标识参数错误；
 * E5501「会员找不到」；E5550「积分异常」（负值防御，严格校验后不可达）；
 * E0001：积分不足（扣减余额不足/无账户非 0 扣减）/ 未查询到相关会员信息（达摩集成）/
 * 达摩CRM积分变更失败或第三方 message 透传（ResourceException 通道）；
 * E5000「未知错误」（增/减执行阶段其他异常兜底，事务回滚）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberPointUpdateFailEnvelope")
public class EcxMemberPointUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5501 会员找不到；E5550 积分异常（不可达防御）；E0001 积分不足/达摩透传；E5000 未知错误；E4004 未授权",
			example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "积分不足")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}