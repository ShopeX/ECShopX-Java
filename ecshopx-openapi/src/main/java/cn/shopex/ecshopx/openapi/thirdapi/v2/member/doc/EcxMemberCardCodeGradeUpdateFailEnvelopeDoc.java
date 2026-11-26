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
 * E5001：手机号必填 / 手机号填写错误 / 会员卡号参数错误 / 等级ID参数错误 / 缺少必要参数（company_id≤0 防御）；
 * E5501「会员找不到」（mobile 查无或 UPDATE affected=0）；
 * E5522「会员卡号已存在」（被其他手机号会员占用）；
 * E5531「会员等级找不到」（membercard_grade 查无）；
 * E5000「系统错误」（其他异常统一兜底，含卡号生成 Redis 3 次冲突失败，事务回滚）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberCardCodeGradeUpdateFailEnvelope")
public class EcxMemberCardCodeGradeUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5501 会员找不到；E5522 会员卡号已存在；E5531 会员等级找不到；E5000 系统错误；E4004 未授权",
			example = "E5522")
	public String code;

	@Schema(description = "错误说明", example = "会员卡号已存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}