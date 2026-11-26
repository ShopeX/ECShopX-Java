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
 * E5001「会员手机号必填」（mobiles 缺失/空白）；
 * E5560「会员手机号格式错误」/「会员标签ID格式错误」（非 JSON 数组/嵌套元素；tag_ids 缺失/空白同报格式错误）；
 * E5501「{手机号}会员不存在」（任一手机号查无，动态文案）；
 * E5561「{标签ID}标签不存在」（任一标签查无，动态文案）；
 * E0001「系统标签不可编辑」（含系统标签，ResourceException 通道）。
 * 校验先于写入：手机号/标签全部解析通过后才逐会员覆盖（逐会员独立事务）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberTagBatchCoverFailEnvelope")
public class EcxMemberTagBatchCoverFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 会员手机号必填；E5560 手机号/标签ID格式错误；E5501 {手机号}会员不存在；E5561 {标签ID}标签不存在；E0001 系统标签不可编辑；E4004 未授权",
			example = "E5501")
	public String code;

	@Schema(description = "错误说明", example = "13800138000会员不存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}